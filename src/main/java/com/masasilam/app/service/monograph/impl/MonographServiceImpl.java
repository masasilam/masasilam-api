package com.masasilam.app.service.monograph.impl;

import com.masasilam.app.config.CacheConfig;
import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.monograph.MonographMapper;
import com.masasilam.app.mapper.newspaper.NewspaperMapper;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.monograph.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.monograph.*;
import com.masasilam.app.service.monograph.MonographService;
import com.masasilam.app.util.HashUtil;
import com.masasilam.app.util.IPUtil;
import com.masasilam.app.util.interceptor.HeaderHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonographServiceImpl implements MonographService {
    private final MonographMapper monographMapper;
    private final NewspaperMapper newspaperMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;
    private static final String SUCCESS = "Success";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
    private static final Set<String> IN_BOOK_ROLES = Set.of("main_arc", "counterpoint", "interlude", "peripheral");
    private static final Set<String> COUNTED_ROLES = Set.of("main_arc", "counterpoint", "interlude", "peripheral", "appendix");
    private static final Set<String> VALID_TEXT_STATUSES = Set.of("complete", "serial", "fragment", "uncertain");
    private static final Set<String> VALID_STATUSES = Set.of("draft", "in_review", "published");
    private static final int MAIN_ARC_WORD_THRESHOLD = 300;
    private static final int PERIPHERAL_WORD_THRESHOLD = 100;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, allEntries = true)
    public DataResponse<MonographDetailResponse> createMonograph(CreateMonographRequest request) {
        try {
            Long sourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            if (sourceId == null) throw new IllegalArgumentException("Sumber koran wajib diisi");

            String slug = resolveUniqueSlug(request.getSlug(), request.getTitle(), request.getEditionDate());

            Monograph monograph = Monograph.builder()
                    .slug(slug)
                    .title(request.getTitle())
                    .subtitle(request.getSubtitle())
                    .newspaperSourceId(sourceId)
                    .editionDate(request.getEditionDate())
                    .editionDateTo(request.getEditionDateTo())
                    .editorialNote(request.getEditorialNote())
                    .colophon(request.getColophon())
                    .centralQuestion(request.getCentralQuestion())
                    .methodNote(request.getMethodNote())
                    .coverImageUrl(request.getCoverImageUrl())
                    .citationFormatTemplate(request.getCitationFormatTemplate())
                    .status("draft")
                    .viewCount(0)
                    .isActive(true)
                    .createdBy(getCurrentUserId())
                    .build();

            monographMapper.insertMonograph(monograph);
            log.info("Monograph created: {} (ID: {}, slug: {})", monograph.getTitle(), monograph.getId(), monograph.getSlug());

            return getMonographDetailById(monograph.getId());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating monograph", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, allEntries = true)
    public DataResponse<MonographDetailResponse> updateMonograph(Long id, UpdateMonographRequest request) {
        try {
            Monograph existing = monographMapper.findById(id);
            if (existing == null) throw new DataNotFoundException();

            Long resolvedSourceId = existing.getNewspaperSourceId();
            Long candidateSourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            if (candidateSourceId != null) resolvedSourceId = candidateSourceId;

            if (request.getSlug() != null && !request.getSlug().isBlank() && !request.getSlug().equals(existing.getSlug())) {
                if (monographMapper.existsBySlugExcluding(request.getSlug(), id)) {
                    throw new IllegalArgumentException("Slug sudah digunakan: " + request.getSlug());
                }
                existing.setSlug(request.getSlug().trim());
            }

            existing.setNewspaperSourceId(resolvedSourceId);
            existing.setTitle(request.getTitle());
            existing.setSubtitle(request.getSubtitle());
            existing.setEditionDate(request.getEditionDate());
            existing.setEditionDateTo(request.getEditionDateTo());
            existing.setEditorialNote(request.getEditorialNote());
            existing.setColophon(request.getColophon());
            existing.setCentralQuestion(request.getCentralQuestion());
            existing.setMethodNote(request.getMethodNote());
            if (request.getCoverImageUrl() != null) existing.setCoverImageUrl(request.getCoverImageUrl());
            existing.setCitationFormatTemplate(request.getCitationFormatTemplate());

            monographMapper.updateMonograph(existing);
            log.info("Monograph updated: {} (ID: {})", existing.getTitle(), id);

            return getMonographDetailById(id);
        } catch (DataNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating monograph ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, allEntries = true)
    public DataResponse<MonographDetailResponse> saveStructure(Long id, MonographStructureRequest request) {
        try {
            Monograph monograph = monographMapper.findById(id);
            if (monograph == null) throw new DataNotFoundException();

            validateStructureInput(request);

            monographMapper.deleteEntriesByMonograph(id);
            monographMapper.deleteChaptersByMonograph(id);
            monographMapper.deleteCriticalNotesByMonograph(id);
            monographMapper.deleteGlossaryByMonograph(id);
            monographMapper.deleteIndexByMonograph(id);

            for (MonographChapterInput chapterInput : request.getChapters()) {
                MonographChapter chapter = MonographChapter.builder()
                        .monographId(id)
                        .chapterOrder(chapterInput.getChapterOrder())
                        .title(chapterInput.getTitle())
                        .narrativeSummary(chapterInput.getNarrativeSummary())
                        .build();
                monographMapper.insertChapter(chapter);
                insertEntries(id, chapter.getId(), "main_arc", chapterInput.getEntries());
            }

            insertEntries(id, null, "counterpoint", request.getCounterpoints());
            insertEntries(id, null, "interlude", request.getInterludes());
            insertEntries(id, null, "peripheral", request.getPeripheralDocuments());
            insertEntries(id, null, "appendix", request.getAppendices());
            insertEntries(id, null, "carry_forward", request.getCarryForwardPool());
            if (request.getCriticalNotes() != null) {
                for (MonographCriticalNoteInput n : request.getCriticalNotes()) {
                    monographMapper.insertCriticalNote(MonographCriticalNote.builder()
                            .monographId(id).noteNumber(n.getNumber())
                            .noteText(n.getNoteText()).relatedArticleId(n.getRelatedArticleId())
                            .build());
                }
            }

            if (request.getGlossary() != null) {
                int order = 0;
                for (MonographGlossaryInput g : request.getGlossary()) {
                    monographMapper.insertGlossaryTerm(MonographGlossaryTerm.builder()
                            .monographId(id).term(g.getTerm()).definition(g.getDefinition())
                            .termOrder(g.getOrder() != null ? g.getOrder() : order++)
                            .build());
                }
            }

            insertIndexBlock(id, "name", request.getNameIndex());
            insertIndexBlock(id, "place", request.getPlaceIndex());
            insertIndexBlock(id, "organization", request.getOrgIndex());

            log.info("Monograph structure assembled: ID {} ({} chapters)", id, request.getChapters().size());
            return getMonographDetailById(id);
        } catch (DataNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error saving structure for monograph ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    private void insertEntries(Long monographId, Long chapterId, String role, List<MonographEntryInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return;
        int order = 0;
        for (MonographEntryInput input : inputs) {
            String textStatus = input.getTextStatus() != null ? input.getTextStatus() : "complete";
            if (!VALID_TEXT_STATUSES.contains(textStatus)) {
                throw new IllegalArgumentException("Status tekstual tidak valid: " + textStatus);
            }
            MonographEntry entry = MonographEntry.builder()
                    .monographId(monographId)
                    .chapterId(chapterId)
                    .articleId(input.getArticleId())
                    .role(role)
                    .textStatus(textStatus)
                    .entryOrder(input.getOrder() != null ? input.getOrder() : order++)
                    .positionLabel(input.getPositionLabel())
                    .columnInfo(input.getColumnInfo())
                    .editorialNote(input.getEditorialNote())
                    .build();
            try {
                monographMapper.insertEntry(entry);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Artikel ID " + input.getArticleId() + " tidak dapat ditautkan (sudah dipakai di monograf ini, atau ID tidak ditemukan).");
            }
        }
    }

    private void insertIndexBlock(Long monographId, String indexType, List<MonographIndexInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return;
        for (MonographIndexInput input : inputs) {
            MonographIndexEntry entry = MonographIndexEntry.builder()
                    .monographId(monographId).indexType(indexType).label(input.getLabel())
                    .build();
            monographMapper.insertIndexEntry(entry);
            for (Long articleId : input.getArticleIds()) {
                monographMapper.insertIndexRef(entry.getId(), articleId);
            }
        }
    }

    private void validateStructureInput(MonographStructureRequest request) {
        if (request.getChapters() == null || request.getChapters().isEmpty()) {
            throw new IllegalArgumentException("Monograf harus memiliki minimal satu bab pada alur dokumenter utama");
        }
        for (MonographChapterInput c : request.getChapters()) {
            if (c.getEntries() == null || c.getEntries().isEmpty()) {
                throw new IllegalArgumentException("Bab '" + c.getTitle() + "' tidak boleh kosong");
            }
        }
    }

    @Override
    @Cacheable(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, key = "#slug")
    public DataResponse<MonographDetailResponse> getMonographDetailBySlug(String slug) {
        try {
            Monograph monograph = monographMapper.findBySlug(slug);
            if (monograph == null || !Boolean.TRUE.equals(monograph.getIsActive())) throw new DataNotFoundException();
            return getMonographDetailById(monograph.getId());
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting monograph detail: {}", slug, e);
            throw new InternalServerErrorException();
        }
    }

    private DataResponse<MonographDetailResponse> getMonographDetailById(Long id) {
        MonographDetailResponse detail = monographMapper.getMonographBaseById(id);
        if (detail == null) throw new DataNotFoundException();

        detail.setEditionDateFormatted(detail.getEditionDate().format(DATE_FORMATTER));

        List<MonographEntryResponse> allEntries = monographMapper.getAllEntries(id);
        List<MonographChapterResponse> chapters = monographMapper.getChaptersByMonograph(id);

        Map<Long, List<MonographEntryResponse>> entriesByChapter = allEntries.stream()
                .filter(e -> e.getChapterId() != null)
                .collect(Collectors.groupingBy(MonographEntryResponse::getChapterId));
        chapters.forEach(c -> c.setEntries(entriesByChapter.getOrDefault(c.getId(), Collections.emptyList())));
        detail.setChapters(chapters);

        Map<String, List<MonographEntryResponse>> byRole = allEntries.stream()
                .collect(Collectors.groupingBy(MonographEntryResponse::getRole));
        detail.setCounterpoints(byRole.getOrDefault("counterpoint", Collections.emptyList()));
        detail.setInterludes(byRole.getOrDefault("interlude", Collections.emptyList()));
        detail.setPeripheralDocuments(byRole.getOrDefault("peripheral", Collections.emptyList()));
        detail.setAppendices(byRole.getOrDefault("appendix", Collections.emptyList()));
        detail.setCarryForwardPool(byRole.getOrDefault("carry_forward", Collections.emptyList()));
        detail.setProvenanceRegister(allEntries);

        int totalArticles = allEntries.size();
        int inBookCount = (int) allEntries.stream().filter(e -> IN_BOOK_ROLES.contains(e.getRole())).count();
        int appendixCount = (int) allEntries.stream().filter(e -> "appendix".equals(e.getRole())).count();
        int wordCount = allEntries.stream()
                .filter(e -> COUNTED_ROLES.contains(e.getRole()))
                .mapToInt(e -> e.getWordCount() != null ? e.getWordCount() : 0).sum();
        detail.setTotalArticles(totalArticles);
        detail.setInBookCount(inBookCount);
        detail.setAppendixCount(appendixCount);
        detail.setWordCount(wordCount);
        detail.setReadingTimeMinutes(Math.max(1, (int) Math.ceil(wordCount / 200.0)));

        detail.setCriticalNotes(monographMapper.getCriticalNotesByMonograph(id));
        detail.setGlossary(monographMapper.getGlossaryByMonograph(id));
        detail.setNameIndex(groupIndexRows(monographMapper.getIndexEntriesByType(id, "name")));
        detail.setPlaceIndex(groupIndexRows(monographMapper.getIndexEntriesByType(id, "place")));
        detail.setOrgIndex(groupIndexRows(monographMapper.getIndexEntriesByType(id, "organization")));

        return new DataResponse<>(SUCCESS, "Monograph detail retrieved successfully", HttpStatus.OK.value(), detail);
    }

    private List<MonographIndexEntryResponse> groupIndexRows(List<MonographIndexEntryRow> rows) {
        Map<String, List<MonographIndexEntryRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(MonographIndexEntryRow::getLabel, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(e -> {
            MonographIndexEntryResponse r = new MonographIndexEntryResponse();
            r.setLabel(e.getKey());
            r.setReferences(e.getValue().stream().map(row -> {
                MonographIndexRefResponse ref = new MonographIndexRefResponse();
                ref.setArticleId(row.getArticleId());
                ref.setArticleSlug(row.getArticleSlug());
                ref.setSourceSlug(row.getSourceSlug());
                ref.setTitle(row.getTitle());
                return ref;
            }).collect(Collectors.toList()));
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public DatatableResponse<MonographResponse> getMonographs(int page, int limit, String sortField, String sortOrder, MonographSearchCriteria criteria) {
        try {
            int offset = (page - 1) * limit;
            List<MonographResponse> list = monographMapper.getMonographs(offset, limit, sortField, sortOrder, criteria);
            list.forEach(m -> m.setEditionDateFormatted(m.getEditionDate().format(DATE_FORMATTER)));
            int totalCount = monographMapper.countMonographs(criteria);
            PageDataResponse<MonographResponse> pageData = new PageDataResponse<>(page, limit, totalCount, list);
            return new DatatableResponse<>(SUCCESS, "Monographs retrieved successfully", HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error getting monographs", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<MonographResponse>> getLatestMonographs(int limit) {
        try {
            List<MonographResponse> list = monographMapper.getLatestMonographs(limit);
            list.forEach(m -> m.setEditionDateFormatted(m.getEditionDate().format(DATE_FORMATTER)));
            return new DataResponse<>(SUCCESS, "Latest monographs retrieved successfully", HttpStatus.OK.value(), list);
        } catch (Exception e) {
            log.error("Error getting latest monographs", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<MonographValidationResponse> validateMonograph(Long id) {
        try {
            Monograph monograph = monographMapper.findById(id);
            if (monograph == null) throw new DataNotFoundException();

            List<String> issues = new ArrayList<>();
            if (monograph.getTitle() == null || monograph.getTitle().isBlank()) issues.add("Judul belum diisi");
            if (monograph.getCoverImageUrl() == null || monograph.getCoverImageUrl().isBlank())
                issues.add("Cover belum diunggah");
            if (monograph.getEditorialNote() == null || monograph.getEditorialNote().isBlank())
                issues.add("Pengantar kuratorial belum diisi");

            int totalEntries = monographMapper.countTotalEntries(id);
            if (totalEntries == 0) issues.add("Belum ada dokumen yang ditautkan ke monograf ini");

            int chapterCount = monographMapper.countChapters(id);
            if (chapterCount == 0) issues.add("Belum ada bab pada alur dokumenter utama");

            int emptyChapters = monographMapper.countChaptersWithoutEntries(id);
            if (emptyChapters > 0) issues.add(emptyChapters + " bab tidak memiliki dokumen di dalamnya");

            int inactiveRefs = monographMapper.countInactiveEntryArticles(id);
            if (inactiveRefs > 0)
                issues.add(inactiveRefs + " dokumen yang ditautkan sudah tidak aktif/dihapus dari arsip koran dan perlu ditinjau ulang");

            MonographValidationResponse response = MonographValidationResponse.builder()
                    .ready(issues.isEmpty())
                    .issues(issues)
                    .build();
            return new DataResponse<>(SUCCESS, "Validation completed", HttpStatus.OK.value(), response);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating monograph ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, allEntries = true)
    public DataResponse<MonographDetailResponse> updateStatus(Long id, UpdateMonographStatusRequest request) {
        try {
            Monograph monograph = monographMapper.findById(id);
            if (monograph == null) throw new DataNotFoundException();

            String newStatus = request.getStatus();
            if (!VALID_STATUSES.contains(newStatus)) {
                throw new IllegalArgumentException("Status tidak valid: " + newStatus);
            }

            if ("published".equals(newStatus)) {
                MonographValidationResponse validation = validateMonograph(id).getData();
                if (!validation.isReady()) {
                    throw new InvalidDataException("Monograf belum siap diterbitkan: " + String.join("; ", validation.getIssues()));
                }
            }

            LocalDateTime publishedAt = "published".equals(newStatus) && monograph.getPublishedAt() == null
                    ? LocalDateTime.now() : monograph.getPublishedAt();

            monographMapper.updateMonographStatus(id, newStatus, publishedAt);
            log.info("Monograph status updated: ID {} -> {}", id, newStatus);
            return getMonographDetailById(id);
        } catch (DataNotFoundException | IllegalArgumentException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating status for monograph ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.MONOGRAPH_DETAIL_CACHE, allEntries = true)
    public DataResponse<Void> deleteMonograph(Long id) {
        try {
            Monograph existing = monographMapper.findById(id);
            if (existing == null) throw new DataNotFoundException();
            monographMapper.softDeleteMonograph(id);
            log.info("Monograph soft-deleted: ID {}", id);
            return new DataResponse<>(SUCCESS, "Monograph deleted successfully", HttpStatus.OK.value(), null);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting monograph ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    public void trackView(String slug, HttpServletRequest request) {
        try {
            Monograph monograph = monographMapper.findBySlug(slug);
            if (monograph == null) return;
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);
            if (!monographMapper.hasViewByHash(viewerHash)) {
                MonographView view = MonographView.builder()
                        .monographId(monograph.getId()).userId(userId)
                        .ipAddress(ipAddress).userAgent(userAgent).viewerHash(viewerHash)
                        .build();
                monographMapper.insertMonographView(view);
                monographMapper.incrementViewCount(monograph.getId());
            }
        } catch (Exception e) {
            log.warn("View tracking failed for monograph {}: {}", slug, e.getMessage());
        }
    }

    @Override
    public List<MonographSitemapItemResponse> getMonographsForSitemap() {
        try {
            return monographMapper.getMonographsForSitemap();
        } catch (Exception e) {
            log.error("Error getting monographs for sitemap", e);
            return Collections.emptyList();
        }
    }

    @Override
    public DataResponse<MonographScaffoldResponse> getScaffold(Long sourceId, LocalDate fromDate, LocalDate toDate) {
        try {
            if (sourceId == null) throw new IllegalArgumentException("sourceId wajib diisi");
            if (fromDate == null || toDate == null)
                throw new IllegalArgumentException("Rentang tanggal (from & to) wajib diisi");
            if (fromDate.isAfter(toDate))
                throw new IllegalArgumentException("Tanggal awal tidak boleh setelah tanggal akhir");

            String sourceName = monographMapper.getSourceNameById(sourceId);
            if (sourceName == null) throw new DataNotFoundException();

            List<MonographScaffoldArticle> candidates = monographMapper.getArticlesForScaffold(sourceId, fromDate, toDate);
            candidates.forEach(a -> a.setSuggestedRole(suggestRole(a.getWordCount(), a.getParentArticleId())));

            int totalWordCount = candidates.stream()
                    .mapToInt(a -> a.getWordCount() != null ? a.getWordCount() : 0)
                    .sum();

            String suggestedTitle = sourceName + " — " + fromDate.format(DATE_FORMATTER)
                    + (fromDate.equals(toDate) ? "" : " s/d " + toDate.format(DATE_FORMATTER));

            MonographScaffoldResponse response = MonographScaffoldResponse.builder()
                    .suggestedTitle(suggestedTitle)
                    .editionDate(fromDate)
                    .editionDateTo(fromDate.equals(toDate) ? null : toDate)
                    .sourceId(sourceId)
                    .sourceName(sourceName)
                    .totalCandidateArticles(candidates.size())
                    .totalWordCountEstimate(totalWordCount)
                    .candidateArticles(candidates)
                    .build();

            return new DataResponse<>(SUCCESS, "Scaffold berhasil dibuat, silakan tinjau sebelum disimpan sebagai monograph", HttpStatus.OK.value(), response);
        } catch (DataNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error building scaffold for source ID: {}", sourceId, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<MonographAdjacentResponse> getAdjacentMonographs(Long id) {
        try {
            Monograph monograph = monographMapper.findById(id);
            if (monograph == null) throw new DataNotFoundException();
            String sourceSlug = monographMapper.getSourceSlugById(monograph.getNewspaperSourceId());
            String sourceName = monographMapper.getSourceNameById(monograph.getNewspaperSourceId());

            MonographAdjacentResponse.MonographNeighbor previous = monographMapper.getPreviousMonograph(
                    monograph.getNewspaperSourceId(), monograph.getEditionDate(), id);
            MonographAdjacentResponse.MonographNeighbor next = monographMapper.getNextMonograph(
                    monograph.getNewspaperSourceId(), monograph.getEditionDate(), id);
            if (previous != null) {
                previous.setEditionDateFormatted(previous.getEditionDate().format(DATE_FORMATTER));
            }
            if (next != null) {
                next.setEditionDateFormatted(next.getEditionDate().format(DATE_FORMATTER));
            }

            MonographAdjacentResponse response = MonographAdjacentResponse.builder()
                    .currentMonographId(id)
                    .newspaperSourceName(sourceName)
                    .newspaperSourceSlug(sourceSlug)
                    .previousMonograph(previous)
                    .nextMonograph(next)
                    .build();

            return new DataResponse<>(SUCCESS, "Adjacent monographs retrieved successfully", HttpStatus.OK.value(), response);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting adjacent monographs for ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    private String suggestRole(Integer wordCount, Long parentArticleId) {
        if (parentArticleId != null) return "carry_forward";
        int wc = wordCount != null ? wordCount : 0;
        if (wc >= MAIN_ARC_WORD_THRESHOLD) return "main_arc";
        if (wc >= PERIPHERAL_WORD_THRESHOLD) return "peripheral";
        return "appendix";
    }

    private Long resolveSourceId(Long sourceId, String sourceName) {
        if (sourceId != null) return sourceId;
        if (sourceName == null || sourceName.isBlank()) return null;
        Long existing = newspaperMapper.findSourceIdByName(sourceName.trim());
        if (existing != null) return existing;
        throw new IllegalArgumentException(
                "Sumber koran '" + sourceName + "' tidak ditemukan. Daftarkan sumber terlebih dahulu di manajemen koran sebelum membuat monograf.");
    }

    private String resolveUniqueSlug(String requestedSlug, String title, LocalDate editionDate) {
        String base = (requestedSlug != null && !requestedSlug.isBlank())
                ? generateSlug(requestedSlug)
                : generateSlug(title + "-" + editionDate);
        String candidate = base;
        int suffix = 2;
        while (monographMapper.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String generateSlug(String text) {
        if (text == null || text.isBlank()) return "";
        return text.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
    }

    private Long getCurrentUserId() {
        try {
            String username = headerHolder.getUsername();
            if (username != null && !username.isEmpty()) {
                User user = userMapper.findUserByUsername(username);
                return user != null ? user.getId() : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}