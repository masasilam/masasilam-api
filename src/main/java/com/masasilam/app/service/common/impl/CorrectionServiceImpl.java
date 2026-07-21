package com.masasilam.app.service.common.impl;

import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.book.BookChapterMapper;
import com.masasilam.app.mapper.book.BookMapper;
import com.masasilam.app.mapper.collaboration.CorrectionMapper;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.request.SubmitCorrectionRequest;
import com.masasilam.app.model.dto.response.CorrectionResponse;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.PageDataResponse;
import com.masasilam.app.model.entity.Book;
import com.masasilam.app.model.entity.BookChapter;
import com.masasilam.app.model.entity.ContentCorrection;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.service.common.CorrectionService;
import com.masasilam.app.service.book.EpubRebuildService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@EnableCaching
@RequiredArgsConstructor
public class CorrectionServiceImpl implements CorrectionService {
    private final CorrectionMapper correctionMapper;
    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;
    private final UserMapper userMapper;
    private final EpubRebuildService epubRebuildService;
    private final CacheManager cacheManager;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String ADMIN = "ADMIN";
    private static final String CHAPTER_BY_PATH = "chapter-by-path";
    private static final String CHAPTER_TEXT = "chapter-text";
    private static final String CHAPTER_LIST = "chapter-list";
    private static final String CHAPTER_PARAGRAPHS = "chapter-paragraphs";

    @Override
    @Transactional
    public DataResponse<CorrectionResponse> submitCorrection(String bookSlug, Integer chapterNumber, SubmitCorrectionRequest request) {
        User user = getCurrentUser();

        Book book = bookMapper.findBookBySlug(bookSlug);
        if (book == null) throw new DataNotFoundException();

        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
        if (chapter == null) throw new DataNotFoundException();

        String originalText = stripInvisibleChars(request.getOriginalText());
        String correctedText = stripInvisibleChars(request.getCorrectedText());
        String contextBefore = stripInvisibleChars(request.getContextBefore());
        String contextAfter = stripInvisibleChars(request.getContextAfter());

        String htmlContent = chapter.getHtmlContent();
        String plainContent = chapter.getContent();

        boolean found = (htmlContent != null && htmlContent.contains(originalText))
                || (plainContent != null && plainContent.contains(originalText));

        if (!found) {
            if (request.getSectionHref() != null && !request.getSectionHref().isBlank()) {
                log.warn("originalText not found in DB content for chapter {}, but sectionHref={} provided — accepting epub submission",
                        chapterNumber, request.getSectionHref());
            } else {
                throw new IllegalArgumentException("Teks yang dilaporkan tidak ditemukan di konten chapter ini. Pastikan Anda memilih teks dengan benar.");
            }
        }

        int duplicateCount = correctionMapper.countDuplicateByUserAndChapter(user.getId(), chapter.getId(), originalText);
        if (duplicateCount > 0) {
            throw new IllegalStateException("Kamu sudah pernah melaporkan teks ini. Tunggu sampai admin meninjau laporan sebelumnya.");
        }

        ContentCorrection correction = new ContentCorrection();
        correction.setBookId(book.getId());
        correction.setChapterId(chapter.getId());
        correction.setSubmittedBy(user.getId());
        correction.setOriginalText(originalText);
        correction.setCorrectedText(correctedText);
        correction.setContextBefore(contextBefore);
        correction.setContextAfter(contextAfter);
        correction.setStartPosition(request.getStartPosition());
        correction.setEndPosition(request.getEndPosition());
        correction.setUserNote(request.getUserNote());
        correction.setEpubCfi(request.getEpubCfi());
        correction.setSectionHref(request.getSectionHref());
        correction.setCreatedAt(LocalDateTime.now());

        correctionMapper.insertCorrection(correction);

        log.info("Correction submitted: user={}, book={}, chapter={}, id={}",
                user.getId(), bookSlug, chapterNumber, correction.getId());

        return new DataResponse<>(SUCCESS,
                "Laporan typo berhasil dikirim. Terima kasih kontribusinya!",
                HttpStatus.CREATED.value(),
                mapToResponse(correction, chapter, book, user));
    }

    @Override
    public DataResponse<List<Integer>> getPendingPositions(String bookSlug, Integer chapterNumber) {
        Book book = bookMapper.findBookBySlug(bookSlug);
        if (book == null) throw new DataNotFoundException();

        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
        if (chapter == null) throw new DataNotFoundException();

        List<Integer> positions = correctionMapper.findPendingPositionsByChapterId(chapter.getId());

        return new DataResponse<>(SUCCESS,
                "Pending positions retrieved",
                HttpStatus.OK.value(),
                positions);
    }

    @Override
    public DatatableResponse<CorrectionResponse> getCorrections(String status, int page, int limit) {
        if (!List.of(PENDING, APPROVED, REJECTED).contains(status)) {
            status = PENDING;
        }

        int offset = (page - 1) * limit;

        User user = getCurrentUser();

        boolean isAdmin = userMapper.findUserRoles(user.getId())
                .stream()
                .anyMatch(role -> ADMIN.equals(role.getName()));

        log.debug("getCorrections: userId={}, isAdmin={}, status={}", user.getId(), isAdmin, status);

        List<ContentCorrection> corrections;
        int total;

        if (isAdmin) {
            corrections = correctionMapper.findByStatus(status, offset, limit);
            total = correctionMapper.countByStatus(status);
        } else {
            corrections = correctionMapper.findByStatusAndUser(status, user.getId(), offset, limit);
            total = correctionMapper.countByStatusAndUser(status, user.getId());
        }

        List<CorrectionResponse> responses = corrections.stream()
                .map(this::mapToResponseFromEntity)
                .toList();

        PageDataResponse<CorrectionResponse> pageData = new PageDataResponse<>(page, limit, total, responses);

        return new DatatableResponse<>(SUCCESS,
                "Corrections retrieved",
                HttpStatus.OK.value(),
                pageData);
    }

    @Override
    @Transactional
    public DataResponse<Void> approveCorrection(Long correctionId) {
        ContentCorrection correction = correctionMapper.findById(correctionId);
        if (correction == null) throw new DataNotFoundException();

        if (!PENDING.equals(correction.getStatus())) {
            throw new IllegalStateException("Koreksi ini sudah diproses sebelumnya (status: " + correction.getStatus() + ")");
        }

        BookChapter chapter = chapterMapper.findChapterById(correction.getChapterId());
        if (chapter == null) throw new DataNotFoundException();

        String updatedHtml = applyCorrection(chapter.getHtmlContent(), correction);

        if (updatedHtml.equals(chapter.getHtmlContent())) {
            log.warn("Correction {} could not be applied — originalText not found in current html_content. Chapter may have been updated already.",
                    correctionId);

            markCorrectionApproved(correction, "APPLIED_AUTOMATICALLY: teks sudah diperbaiki sebelumnya");

            return new DataResponse<>(SUCCESS,
                    "Koreksi disetujui (teks sudah diperbaiki sebelumnya)",
                    HttpStatus.OK.value(), null);
        }

        chapter.setHtmlContent(updatedHtml);
        chapter.setContent(Jsoup.parse(updatedHtml).text());
        chapter.setUpdatedAt(LocalDateTime.now());
        chapterMapper.updateChapter(chapter);

        log.info("Chapter {} updated with correction {}", chapter.getId(), correctionId);

        markCorrectionApproved(correction, null);

        evictChapterCaches();

        epubRebuildService.rebuildAsync(chapter.getBookId());

        log.info("Correction {} approved, epub rebuild triggered for book {}", correctionId, chapter.getBookId());

        return new DataResponse<>(SUCCESS,
                "Koreksi disetujui. Epub sedang diperbarui di background.",
                HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> rejectCorrection(Long correctionId, String note) {
        ContentCorrection correction = correctionMapper.findById(correctionId);
        if (correction == null) throw new DataNotFoundException();

        if (!PENDING.equals(correction.getStatus())) {
            throw new IllegalStateException("Koreksi ini sudah diproses (status: " + correction.getStatus() + ")");
        }

        User admin = getCurrentUser();

        correction.setStatus(REJECTED);
        correction.setReviewedBy(admin.getId());
        correction.setReviewNote(note);
        correction.setReviewedAt(LocalDateTime.now());

        correctionMapper.updateCorrection(correction);

        log.info("Correction {} rejected by admin {}", correctionId, admin.getId());

        return new DataResponse<>(SUCCESS,
                "Koreksi ditolak.",
                HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<List<CorrectionResponse>> getMyPendingForBook(String bookSlug) {
        User user = getCurrentUser();
        Book book = bookMapper.findBookBySlug(bookSlug);
        if (book == null) throw new DataNotFoundException();

        List<ContentCorrection> list = correctionMapper.findPendingByBookAndUser(book.getId(), user.getId());

        List<CorrectionResponse> responses = list.stream()
                .map(this::mapToResponseFromEntity)
                .toList();

        return new DataResponse<>("Success", "Pending corrections retrieved",
                HttpStatus.OK.value(), responses);
    }

    private String applyCorrection(String html, ContentCorrection correction) {
        if (correction.getContextBefore() != null
                && !correction.getContextBefore().isBlank()
                && correction.getContextAfter() != null
                && !correction.getContextAfter().isBlank()) {

            String searchString = correction.getContextBefore()
                    + correction.getOriginalText()
                    + correction.getContextAfter();

            String replaceString = correction.getContextBefore()
                    + correction.getCorrectedText()
                    + correction.getContextAfter();

            if (html.contains(searchString)) {
                log.info("Correction applied using context strategy");
                return html.replace(searchString, replaceString);
            }

            String searchNormalized = normalizeWhitespace(searchString);
            String htmlNormalized = normalizeWhitespace(html);

            if (htmlNormalized.contains(searchNormalized)) {
                log.warn("Context found only after normalization, falling back to direct replace for correction {}", correction.getId());
            }
        }

        if (html.contains(correction.getOriginalText())) {
            long occurrences = countOccurrences(html, correction.getOriginalText());

            if (occurrences > 1) {
                log.warn("originalText '{}' found {} times in chapter. Replacing first occurrence only. Consider providing contextBefore/contextAfter.",
                        correction.getOriginalText(), occurrences);
                return html.replaceFirst(
                        java.util.regex.Pattern.quote(correction.getOriginalText()),
                        java.util.regex.Matcher.quoteReplacement(correction.getCorrectedText()));
            }

            log.info("Correction applied using direct replace strategy");
            return html.replace(correction.getOriginalText(), correction.getCorrectedText());
        }

        log.warn("Could not apply correction {}: originalText not found", correction.getId());
        return html;
    }

    private long countOccurrences(String text, String search) {
        if (text == null || search == null || search.isEmpty()) return 0;
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
    }

    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private void markCorrectionApproved(ContentCorrection correction, String autoNote) {
        User admin = getCurrentUserOrNull();
        correction.setStatus(APPROVED);
        correction.setReviewedBy(admin != null ? admin.getId() : null);
        correction.setReviewNote(autoNote);
        correction.setReviewedAt(LocalDateTime.now());
        correctionMapper.updateCorrection(correction);
    }

    private void evictChapterCaches() {
        List.of(CHAPTER_BY_PATH, CHAPTER_TEXT, CHAPTER_LIST, CHAPTER_PARAGRAPHS)
                .forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        log.debug("Cache '{}' evicted", cacheName);
                    }
                });
        log.info("All chapter caches evicted after correction approval");
    }

    private CorrectionResponse mapToResponseFromEntity(ContentCorrection c) {
        CorrectionResponse r = new CorrectionResponse();
        r.setId(c.getId());
        r.setBookId(c.getBookId());
        r.setBookTitle(c.getBookTitle());
        r.setChapterId(c.getChapterId());
        r.setChapterTitle(c.getChapterTitle());
        r.setChapterNumber(c.getChapterNumber());
        r.setOriginalText(c.getOriginalText());
        r.setCorrectedText(c.getCorrectedText());
        r.setContextBefore(c.getContextBefore());
        r.setContextAfter(c.getContextAfter());
        r.setStartPosition(c.getStartPosition());
        r.setEndPosition(c.getEndPosition());
        r.setUserNote(c.getUserNote());
        r.setStatus(c.getStatus());
        r.setSubmittedBy(c.getSubmittedBy());
        r.setSubmittedByUsername(c.getSubmittedByUsername());
        r.setReviewedBy(c.getReviewedBy());
        r.setReviewNote(c.getReviewNote());
        r.setCreatedAt(c.getCreatedAt());
        r.setReviewedAt(c.getReviewedAt());
        r.setEpubCfi(c.getEpubCfi());
        r.setSectionHref(c.getSectionHref());
        r.setDiffPreview(buildDiffPreview(c));
        return r;
    }

    private CorrectionResponse mapToResponse(ContentCorrection c, BookChapter chapter, Book book, User user) {
        CorrectionResponse r = new CorrectionResponse();
        r.setId(c.getId());
        r.setBookId(book.getId());
        r.setBookTitle(book.getTitle());
        r.setChapterId(chapter.getId());
        r.setChapterTitle(chapter.getTitle());
        r.setChapterNumber(chapter.getChapterNumber());
        r.setOriginalText(c.getOriginalText());
        r.setCorrectedText(c.getCorrectedText());
        r.setContextBefore(c.getContextBefore());
        r.setContextAfter(c.getContextAfter());
        r.setStartPosition(c.getStartPosition());
        r.setEndPosition(c.getEndPosition());
        r.setUserNote(c.getUserNote());
        r.setStatus(PENDING);
        r.setSubmittedBy(user.getId());
        r.setSubmittedByUsername(user.getUsername());
        r.setEpubCfi(c.getEpubCfi());
        r.setSectionHref(c.getSectionHref());
        r.setCreatedAt(c.getCreatedAt());
        r.setDiffPreview(buildDiffPreview(c));
        return r;
    }

    private String buildDiffPreview(ContentCorrection c) {
        String before = Objects.toString(c.getContextBefore(), "");
        String after = Objects.toString(c.getContextAfter(), "");

        String originalLine = before + "[" + c.getOriginalText() + "]" + after;
        String correctedLine = before + "[" + c.getCorrectedText() + "]" + after;

        return originalLine + " → " + correctedLine;
    }

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) {
            throw new UnauthorizedException();
        }
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private String stripInvisibleChars(String text) {
        if (text == null) return null;
        return text
                .replace("\u00AD", "")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\uFEFF", "")
                .replaceAll("[\\p{Cf}]", "");
    }
}