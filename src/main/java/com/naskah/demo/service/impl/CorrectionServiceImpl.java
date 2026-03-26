package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.BookChapterMapper;
import com.naskah.demo.mapper.BookMapper;
import com.naskah.demo.mapper.CorrectionMapper;
import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.dto.request.SubmitCorrectionRequest;
import com.naskah.demo.model.dto.response.CorrectionResponse;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.DatatableResponse;
import com.naskah.demo.model.dto.response.PageDataResponse;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.model.entity.ContentCorrection;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.service.CorrectionService;
import com.naskah.demo.service.book.EpubRebuildService;
import com.naskah.demo.util.interceptor.HeaderHolder;
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

    private final CorrectionMapper    correctionMapper;
    private final BookMapper          bookMapper;
    private final BookChapterMapper   chapterMapper;
    private final UserMapper          userMapper;
    private final EpubRebuildService  epubRebuildService;
    private final CacheManager        cacheManager;
    private final HeaderHolder        headerHolder;

    private static final String SUCCESS = "Success";

    // ── USER: SUBMIT ─────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<CorrectionResponse> submitCorrection(
            String bookSlug,
            Integer chapterNumber,
            SubmitCorrectionRequest request) {

        // 1. Validasi user login
        User user = getCurrentUser();

        // 2. Validasi buku ada
        Book book = bookMapper.findBookBySlug(bookSlug);
        if (book == null) throw new DataNotFoundException();

        // 3. Validasi chapter ada
        BookChapter chapter = chapterMapper.findChapterByNumber(
                book.getId(), chapterNumber);
        if (chapter == null) throw new DataNotFoundException();

        // 4. Validasi: originalText harus ada di html_content
        //    Cegah laporan yang tidak valid sama sekali
        String htmlContent = chapter.getHtmlContent();
        if (htmlContent == null || !htmlContent.contains(request.getOriginalText())) {
            throw new IllegalArgumentException(
                    "Teks yang dilaporkan tidak ditemukan di konten chapter ini. " +
                            "Pastikan Anda memilih teks dengan benar.");
        }

        // 5. Cek duplikat — user tidak bisa lapor teks yang sama 2x
        int duplicateCount = correctionMapper.countDuplicateByUserAndChapter(
                user.getId(), chapter.getId(), request.getOriginalText());

        if (duplicateCount > 0) {
            throw new IllegalStateException(
                    "Kamu sudah pernah melaporkan teks ini. " +
                            "Tunggu sampai admin meninjau laporan sebelumnya.");
        }

        // 6. Insert laporan
        ContentCorrection correction = new ContentCorrection();
        correction.setBookId(book.getId());
        correction.setChapterId(chapter.getId());
        correction.setSubmittedBy(user.getId());
        correction.setOriginalText(request.getOriginalText());
        correction.setCorrectedText(request.getCorrectedText());
        correction.setContextBefore(request.getContextBefore());
        correction.setContextAfter(request.getContextAfter());
        correction.setStartPosition(request.getStartPosition());
        correction.setEndPosition(request.getEndPosition());
        correction.setUserNote(request.getUserNote());
        correction.setCreatedAt(LocalDateTime.now());

        correctionMapper.insertCorrection(correction);

        log.info("Correction submitted: user={}, book={}, chapter={}, id={}",
                user.getId(), bookSlug, chapterNumber, correction.getId());

        return new DataResponse<>(SUCCESS,
                "Laporan typo berhasil dikirim. Terima kasih kontribusinya!",
                HttpStatus.CREATED.value(),
                mapToResponse(correction, chapter, book, user));
    }

    // ── USER: PENDING POSITIONS ──────────────────────────────

    @Override
    public DataResponse<List<Integer>> getPendingPositions(
            String bookSlug, Integer chapterNumber) {

        Book book = bookMapper.findBookBySlug(bookSlug);
        if (book == null) throw new DataNotFoundException();

        BookChapter chapter = chapterMapper.findChapterByNumber(
                book.getId(), chapterNumber);
        if (chapter == null) throw new DataNotFoundException();

        List<Integer> positions = correctionMapper
                .findPendingPositionsByChapterId(chapter.getId());

        return new DataResponse<>(SUCCESS,
                "Pending positions retrieved",
                HttpStatus.OK.value(),
                positions);
    }

    // ── ADMIN: LIST ──────────────────────────────────────────

    @Override
    public DatatableResponse<CorrectionResponse> getPendingCorrections(
            String status, int page, int limit) {

        // Validasi status
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(status)) {
            status = "PENDING";
        }

        int offset = (page - 1) * limit;

        List<ContentCorrection> corrections = correctionMapper
                .findByStatus(status, offset, limit);

        int total = correctionMapper.countByStatus(status);

        List<CorrectionResponse> responses = corrections.stream()
                .map(this::mapToResponseFromEntity)
                .toList();

        PageDataResponse<CorrectionResponse> pageData =
                new PageDataResponse<>(page, limit, total, responses);

        return new DatatableResponse<>(SUCCESS,
                "Corrections retrieved",
                HttpStatus.OK.value(),
                pageData);
    }

    // ── ADMIN: APPROVE ───────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<Void> approveCorrection(Long correctionId) {

        // 1. Ambil dan validasi koreksi
        ContentCorrection correction = correctionMapper.findById(correctionId);
        if (correction == null) throw new DataNotFoundException();

        if (!"PENDING".equals(correction.getStatus())) {
            throw new IllegalStateException(
                    "Koreksi ini sudah diproses sebelumnya (status: "
                            + correction.getStatus() + ")");
        }

        // 2. Ambil chapter dari DB
        BookChapter chapter = chapterMapper.findChapterById(
                correction.getChapterId());
        if (chapter == null) throw new DataNotFoundException();

        // 3. Terapkan koreksi ke html_content
        String updatedHtml = applyCorrection(
                chapter.getHtmlContent(), correction);

        // 4. Validasi hasil — pastikan replace berhasil
        //    (teks asli tidak boleh ada lagi di hasil)
        if (updatedHtml.equals(chapter.getHtmlContent())) {
            // Replace tidak berhasil — teks tidak ditemukan
            // Bisa terjadi jika chapter sudah dikoreksi sebelumnya
            log.warn("Correction {} could not be applied — " +
                            "originalText not found in current html_content. " +
                            "Chapter may have been updated already.",
                    correctionId);

            // Tetap mark sebagai APPROVED tapi catat di review_note
            markCorrectionApproved(correction,
                    "APPLIED_AUTOMATICALLY: teks sudah diperbaiki sebelumnya");

            return new DataResponse<>(SUCCESS,
                    "Koreksi disetujui (teks sudah diperbaiki sebelumnya)",
                    HttpStatus.OK.value(), null);
        }

        // 5. Update html_content dan plain content di DB
        chapter.setHtmlContent(updatedHtml);
        chapter.setContent(Jsoup.parse(updatedHtml).text()); // update plain text
        chapter.setUpdatedAt(LocalDateTime.now());
        chapterMapper.updateChapter(chapter);

        log.info("Chapter {} updated with correction {}",
                chapter.getId(), correctionId);

        // 6. Mark koreksi sebagai APPROVED
        markCorrectionApproved(correction, null);

        // 7. Evict semua cache yang berkaitan dengan chapter ini
        evictChapterCaches();

        // 8. Trigger async rebuild epub
        //    Admin tidak menunggu ini — response sudah dikirim duluan
        //    Epub di-rebuild di background thread, lalu di-upload ke Cloudinary
        //    (overwrite URL lama → EpubReaderPage otomatis dapat file baru)
        epubRebuildService.rebuildAsync(chapter.getBookId());

        log.info("Correction {} approved, epub rebuild triggered for book {}",
                correctionId, chapter.getBookId());

        return new DataResponse<>(SUCCESS,
                "Koreksi disetujui. Epub sedang diperbarui di background.",
                HttpStatus.OK.value(), null);
    }

    // ── ADMIN: REJECT ────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<Void> rejectCorrection(Long correctionId, String note) {

        ContentCorrection correction = correctionMapper.findById(correctionId);
        if (correction == null) throw new DataNotFoundException();

        if (!"PENDING".equals(correction.getStatus())) {
            throw new IllegalStateException(
                    "Koreksi ini sudah diproses (status: " + correction.getStatus() + ")");
        }

        // Ambil admin ID dari context
        User admin = getCurrentUser();

        correction.setStatus("REJECTED");
        correction.setReviewedBy(admin.getId());
        correction.setReviewNote(note);
        correction.setReviewedAt(LocalDateTime.now());

        correctionMapper.updateCorrection(correction);

        log.info("Correction {} rejected by admin {}", correctionId, admin.getId());

        return new DataResponse<>(SUCCESS,
                "Koreksi ditolak.",
                HttpStatus.OK.value(), null);
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────

    /**
     * Terapkan koreksi ke html_content dengan strategi berlapis:
     *
     * Strategi 1: Replace menggunakan KONTEKS (paling presisi)
     *   Cari: contextBefore + originalText + contextAfter
     *   Ganti: contextBefore + correctedText + contextAfter
     *   → Aman meski originalText muncul di banyak tempat
     *
     * Strategi 2: Fallback tanpa konteks (kalau konteks tidak tersedia)
     *   Cari: originalText
     *   Ganti: correctedText
     *   → Hanya dipakai jika konteks tidak ada
     *   → Lebih berisiko jika teks sama muncul berkali-kali
     */
    private String applyCorrection(String html, ContentCorrection correction) {

        // Strategi 1: pakai konteks jika tersedia
        if (correction.getContextBefore() != null
                && !correction.getContextBefore().isBlank()
                && correction.getContextAfter() != null
                && !correction.getContextAfter().isBlank()) {

            String searchString  = correction.getContextBefore()
                    + correction.getOriginalText()
                    + correction.getContextAfter();

            String replaceString = correction.getContextBefore()
                    + correction.getCorrectedText()
                    + correction.getContextAfter();

            if (html.contains(searchString)) {
                log.info("Correction applied using context strategy");
                return html.replace(searchString, replaceString);
            }

            // Konteks tersedia tapi tidak cocok persis
            // Mungkin ada whitespace/newline — coba strategi 1b
            // Normalize whitespace di konteks
            String searchNormalized  = normalizeWhitespace(searchString);
            String htmlNormalized    = normalizeWhitespace(html);

            if (htmlNormalized.contains(searchNormalized)) {
                // Tidak bisa replace langsung di normalized
                // Fallback ke strategi 2 dengan logging warning
                log.warn("Context found only after normalization, " +
                                "falling back to direct replace for correction {}",
                        correction.getId());
            }
        }

        // Strategi 2: replace langsung tanpa konteks
        if (html.contains(correction.getOriginalText())) {
            // Jika muncul lebih dari sekali, replace PERTAMA saja
            // (berdasarkan startPosition jika tersedia)
            long occurrences = countOccurrences(html, correction.getOriginalText());

            if (occurrences > 1) {
                log.warn("originalText '{}' found {} times in chapter. " +
                                "Replacing first occurrence only. " +
                                "Consider providing contextBefore/contextAfter.",
                        correction.getOriginalText(), occurrences);
                return html.replaceFirst(
                        java.util.regex.Pattern.quote(correction.getOriginalText()),
                        java.util.regex.Matcher.quoteReplacement(correction.getCorrectedText()));
            }

            log.info("Correction applied using direct replace strategy");
            return html.replace(
                    correction.getOriginalText(),
                    correction.getCorrectedText());
        }

        // Tidak berhasil — kembalikan HTML asli
        log.warn("Could not apply correction {}: originalText not found",
                correction.getId());
        return html;
    }

    /**
     * Helper: hitung berapa kali substring muncul di string.
     */
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

    /**
     * Helper: normalize whitespace (ganti newline/tab dengan spasi).
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Update status correction ke APPROVED.
     * Dipanggil setelah html_content berhasil diupdate.
     */
    private void markCorrectionApproved(ContentCorrection correction,
                                        String autoNote) {
        User admin = getCurrentUserOrNull();
        correction.setStatus("APPROVED");
        correction.setReviewedBy(admin != null ? admin.getId() : null);
        correction.setReviewNote(autoNote);
        correction.setReviewedAt(LocalDateTime.now());
        correctionMapper.updateCorrection(correction);
    }

    /**
     * Evict semua cache yang menyimpan konten chapter.
     *
     * Cache yang dievict:
     *  - chapter-by-path : hasil readChapterBySlugPath()
     *  - chapter-text    : hasil getChapterTextForTTS()
     *  - chapter-list    : hasil getAllChaptersSummary()
     *  - chapter-paragraphs : hasil getChapterParagraphs()
     *
     * Setelah evict, request berikutnya akan ambil dari DB
     * → user otomatis dapat konten terbaru
     */
    private void evictChapterCaches() {
        List.of("chapter-by-path", "chapter-text",
                        "chapter-list", "chapter-paragraphs")
                .forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        log.debug("Cache '{}' evicted", cacheName);
                    }
                });
        log.info("All chapter caches evicted after correction approval");
    }

    /**
     * Map ContentCorrection entity ke CorrectionResponse DTO.
     * Versi ini dipakai saat data sudah ada dari DB dengan JOIN.
     */
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

        // Build diff preview untuk UI admin
        // Format: "...contextBefore [SALAH] contextAfter..."
        r.setDiffPreview(buildDiffPreview(c));

        return r;
    }

    /**
     * Map saat data belum ada JOIN (langsung dari insert).
     */
    private CorrectionResponse mapToResponse(
            ContentCorrection c, BookChapter chapter,
            Book book, User user) {

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
        r.setStatus("PENDING");
        r.setSubmittedBy(user.getId());
        r.setSubmittedByUsername(user.getUsername());
        r.setCreatedAt(c.getCreatedAt());
        r.setDiffPreview(buildDiffPreview(c));
        return r;
    }

    /**
     * Build diff preview untuk admin panel.
     *
     * Contoh output:
     * "...mereka saling [dipandanag] dengan penuh..."
     *                        ↓
     * "...mereka saling [dipandang] dengan penuh..."
     */
    private String buildDiffPreview(ContentCorrection c) {
        String before = Objects.toString(c.getContextBefore(), "");
        String after  = Objects.toString(c.getContextAfter(), "");

        String originalLine  = before + "[" + c.getOriginalText() + "]" + after;
        String correctedLine = before + "[" + c.getCorrectedText() + "]" + after;

        return originalLine + " → " + correctedLine;
    }

    // ── AUTH HELPERS ─────────────────────────────────────────

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
}