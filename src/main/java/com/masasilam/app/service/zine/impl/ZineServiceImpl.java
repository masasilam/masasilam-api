package com.masasilam.app.service.zine.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.ForbiddenException;
import com.masasilam.app.exception.custom.InternalServerErrorException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.*;
import com.masasilam.app.model.dto.*;
import com.masasilam.app.model.dto.request.ZineRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.*;
import com.masasilam.app.repository.ChapterRepository;
import com.masasilam.app.service.book.EpubService;
import com.masasilam.app.service.zine.ZineService;
import com.masasilam.app.util.HashUtil;
import com.masasilam.app.util.IPUtil;
import com.masasilam.app.util.file.EpubMetadataExtractor;
import com.masasilam.app.util.file.FileUtil;
import com.masasilam.app.util.interceptor.HeaderHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZineServiceImpl implements ZineService {
    private final ZineMapper zineMapper;
    private final AuthorMapper authorMapper;
    private final ContributorMapper contributorMapper;
    private final LanguageMapper languageMapper;
    private final CopyrightStatusMapper copyrightStatusMapper;
    private final UserMapper userMapper;
    private final GenreMapper genreMapper;
    private final HeaderHolder headerHolder;
    private final EpubService epubService;
    private final ChapterRepository zineChapterRepository;
    private final FileUtil fileUtil;

    private static final String SUCCESS = "Success";

    @Value("${file.upload.max-size:52428800}")
    private String maxFileSizeStr;

    @Override
    @Transactional
    public DataResponse<ZineResponse> createZine(ZineRequest request) {
        try {
            if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty()) {
                throw new UnauthorizedException();
            }
            if (headerHolder.getRoles() == null || !Arrays.asList(headerHolder.getRoles()).contains("ADMIN")) {
                throw new ForbiddenException();
            }

            long maxSizeBytes = fileUtil.parseFileSize(maxFileSizeStr);
            fileUtil.validateFile(request.getZineFile(), maxSizeBytes);

            String fileExtension = fileUtil.getFileExtension(request.getZineFile().getOriginalFilename());
            if (!"epub".equalsIgnoreCase(fileExtension)) {
                throw new IllegalArgumentException("Hanya file EPUB yang didukung untuk zine");
            }

            log.info("Extracting metadata from EPUB zine file");
            CompleteEpubMetadata epubMeta = EpubMetadataExtractor.extractCompleteMetadata(
                    request.getZineFile().getInputStream());

            String finalTitle = epubMeta.getTitle();
            if (finalTitle == null || finalTitle.isEmpty()) {
                throw new IllegalArgumentException("Judul tidak ditemukan di metadata EPUB.");
            }
            if (epubMeta.getPublisher() == null || epubMeta.getPublisher().isEmpty()) {
                throw new IllegalArgumentException("Publisher tidak ditemukan di metadata EPUB.");
            }
            if (epubMeta.getPublicationYear() == null) {
                throw new IllegalArgumentException("Tahun publikasi tidak ditemukan di metadata EPUB.");
            }

            String titleWithSubtitle = epubMeta.getSubtitle() != null && !epubMeta.getSubtitle().isEmpty()
                    ? finalTitle + " " + epubMeta.getSubtitle()
                    : finalTitle;
            String baseSlug = fileUtil.sanitizeFilename(titleWithSubtitle);
            Zine existingZine = checkExistingZineWithSameAuthor(baseSlug, epubMeta);

            if (existingZine != null) {
                log.info("Found existing zine '{}', updating instead.", baseSlug);
                return updateExistingZine(existingZine, request.getZineFile(), epubMeta);
            }

            String finalSlug = baseSlug;
            int duplicateCount = zineMapper.countBySlug(finalSlug);
            if (duplicateCount > 0) {
                finalSlug = baseSlug + "-" + System.currentTimeMillis();
            }

            Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
            CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

            FileStorageResult zineResult = fileUtil.saveAndUploadBookFile(request.getZineFile(), finalTitle);
            BookMetadata metadata = fileUtil.extractBookMetadata(request.getZineFile());

            Zine zine = new Zine();
            zine.setTitle(finalTitle);
            zine.setSlug(finalSlug);
            zine.setSubtitle(epubMeta.getSubtitle());
            zine.setCollectionName(epubMeta.getCollectionName());
            zine.setVolume(calculateVolume(epubMeta.getCollectionName(), epubMeta.getPublicationYear()));

            if (epubMeta.getIssueNumber() != null) {
                zine.setIssueNumber(String.valueOf(epubMeta.getIssueNumber()));
            }
            zine.setFirstPublisher(epubMeta.getFirstPublisher());
            zine.setFirstPublishedDate(epubMeta.getFirstPublished());
            zine.setPublicationYear(epubMeta.getPublicationYear());
            zine.setPublisher(epubMeta.getPublisher());
            zine.setLanguageId(Long.valueOf(language.getId()));
            zine.setDescription(epubMeta.getDescription());
            zine.setFileUrl(zineResult.getCloudUrl());
            zine.setSource(epubMeta.getSource());
            zine.setFileFormat(metadata.getFileFormat());
            zine.setFileSize(metadata.getFileSize());
            zine.setCopyrightStatusId(Long.valueOf(copyrightStatus.getId()));
            zine.setViewCount(0);
            zine.setReadCount(0);
            zine.setDownloadCount(0);
            zine.setIsActive(true);
            zine.setIsFeatured(false);
            zine.setPublishedAt(epubMeta.getPublishedAt() != null
                    ? epubMeta.getPublishedAt().atStartOfDay().atOffset(ZoneOffset.UTC)
                    : null);
            zine.setCategory(epubMeta.getCategory());
            zine.setCreatedAt(epubMeta.getUpdatedAt() != null
                    ? epubMeta.getUpdatedAt().atOffset(ZoneOffset.UTC)
                    : OffsetDateTime.now(ZoneOffset.UTC));
            zine.setUpdatedAt(epubMeta.getUpdatedAt() != null
                    ? epubMeta.getUpdatedAt().atOffset(ZoneOffset.UTC)
                    : OffsetDateTime.now(ZoneOffset.UTC));

            zineMapper.insertZine(zine);
            log.info("Zine created with ID: {} and slug: {}", zine.getId(), zine.getSlug());

            Book adapter = toBookAdapter(zine);
            EpubProcessResult result = epubService.processEpubFile(request.getZineFile(), adapter, zineChapterRepository);
            log.info("EPUB zine processed: {} chapters, {} words", result.getTotalChapters(), result.getTotalWords());

            zine.setTotalWord((int) result.getTotalWords());
            zine.setTotalPages(result.getTotalChapters());
            zine.setEstimatedReadTime(fileUtil.calculateEstimatedReadTime(result.getTotalWords()));
            zine.setCoverImageUrl(result.getCoverImageUrl());
            zine.setFileUrlArchive(zine.getFileUrl());
            zineMapper.updateZine(zine);

            genreProcessing(epubMeta, zine);
            authorProcessing(epubMeta, zine);
            contributorProcessing(epubMeta, zine);

            ZineResponse data = zineMapper.getZineDetailBySlug(zine.getSlug());
            log.info("Zine berhasil dibuat: {}", finalTitle);

            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, 201, data);

        } catch (Exception e) {
            log.error("Error creating zine: {}", e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }

    @Transactional
    public DataResponse<ZineResponse> updateExistingZine(Zine existingZine, MultipartFile newFile, CompleteEpubMetadata epubMeta) throws IOException {
        log.info("Updating existing zine ID: {} - {}", existingZine.getId(), existingZine.getTitle());

        if (existingZine.getFileUrl() != null) fileUtil.deleteFile(existingZine.getFileUrl());
        if (existingZine.getCoverImageUrl() != null) fileUtil.deleteFile(existingZine.getCoverImageUrl());

        zineMapper.deleteZineGenres(existingZine.getId());
        zineMapper.deleteZineContributors(existingZine.getId());

        FileStorageResult zineResult = fileUtil.saveAndUploadBookFile(newFile, existingZine.getTitle());
        BookMetadata metadata = fileUtil.extractBookMetadata(newFile);

        Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
        CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

        existingZine.setTitle(epubMeta.getTitle());
        existingZine.setSubtitle(epubMeta.getSubtitle());
        existingZine.setPublicationYear(epubMeta.getPublicationYear());
        existingZine.setPublisher(epubMeta.getPublisher());
        existingZine.setLanguageId(Long.valueOf(language.getId()));
        existingZine.setDescription(epubMeta.getDescription());
        existingZine.setFileUrl(zineResult.getCloudUrl());
        existingZine.setSource(epubMeta.getSource());
        existingZine.setFileFormat(metadata.getFileFormat());
        existingZine.setFileSize(metadata.getFileSize());
        existingZine.setCopyrightStatusId(Long.valueOf(copyrightStatus.getId()));
        existingZine.setPublishedAt(epubMeta.getPublishedAt() != null
                ? epubMeta.getPublishedAt().atStartOfDay().atOffset(ZoneOffset.UTC)
                : null);
        existingZine.setCategory(epubMeta.getCategory());
        existingZine.setCollectionName(epubMeta.getCollectionName());
        existingZine.setVolume(calculateVolume(epubMeta.getCollectionName(), epubMeta.getPublicationYear()));

        if (epubMeta.getIssueNumber() != null) {
            existingZine.setIssueNumber(String.valueOf(epubMeta.getIssueNumber()));
        }
        existingZine.setFirstPublisher(epubMeta.getFirstPublisher());
        existingZine.setFirstPublishedDate(epubMeta.getFirstPublished());
        existingZine.setUpdatedAt(epubMeta.getUpdatedAt() != null
                ? epubMeta.getUpdatedAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC));

        Book adapter = toBookAdapter(existingZine);
        EpubProcessResult result = epubService.processEpubFileForUpdate(newFile, adapter, zineChapterRepository);

        existingZine.setTotalWord((int) result.getTotalWords());
        existingZine.setTotalPages(result.getTotalChapters());
        existingZine.setEstimatedReadTime(fileUtil.calculateEstimatedReadTime(result.getTotalWords()));
        existingZine.setCoverImageUrl(result.getCoverImageUrl());

        zineMapper.updateZine(existingZine);

        genreProcessing(epubMeta, existingZine);
        authorProcessing(epubMeta, existingZine);
        contributorProcessing(epubMeta, existingZine);

        ZineResponse data = zineMapper.getZineDetailBySlug(existingZine.getSlug());
        return new DataResponse<>(SUCCESS, "Zine berhasil diperbarui", 200, data);
    }

    @Override
    @Transactional
    public DataResponse<ZineResponse> getZineDetailBySlug(String slug, HttpServletRequest request) {
        try {
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String userType = userId != null ? "authenticated (userId: " + userId + ")" : "guest";
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);

            Long zineId = zineMapper.getZineIdBySlug(slug);
            if (zineId == null) throw new DataNotFoundException();

            boolean hasViewed = userId != null
                    ? zineMapper.hasActionByUserAndZine(zineId, userId, "view")
                    : zineMapper.hasActionByHash(viewerHash, "view");

            if (!hasViewed) {
                try {
                    zineMapper.insertAction(ZineView.builder()
                            .zineId(zineId).slug(slug).userId(userId)
                            .ipAddress(ipAddress).userAgent(userAgent)
                            .viewerHash(viewerHash).actionType("view")
                            .build());
                    zineMapper.incrementViewCountBySlug(slug);
                    log.info("✓ New view recorded for slug: {} by {}", slug, userType);
                } catch (DuplicateKeyException e) {
                    log.warn("Race condition on view insert for slug: {} by {} — skipping", slug, userType);
                }
            } else {
                log.info("✗ Duplicate view detected for slug: {} by {} - NOT incrementing", slug, userType);
            }

            ZineResponse data = zineMapper.getZineDetailBySlug(slug);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
            }
            throw new DataNotFoundException();

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching zine detail for slug: {}", slug, e);
            throw new RuntimeException("Failed to fetch zine detail", e);
        }
    }

    @Override
    public DatatableResponse<ZineResponse> getPaginatedZines(int page, int limit, String sortField, String sortOrder, ZineSearchCriteria criteria) {
        Map<String, String> allowedSortFields = new HashMap<>();
        allowedSortFields.put("updateAt", "z.updated_at");
        allowedSortFields.put("title", "z.title");
        allowedSortFields.put("publishedAt", "z.published_at");
        allowedSortFields.put("estimatedReadTime", "z.estimated_read_time");
        allowedSortFields.put("totalWord", "z.total_word");
        allowedSortFields.put("averageRating", "average_rating");
        allowedSortFields.put("viewCount", "z.view_count");
        allowedSortFields.put("readCount", "z.read_count");
        allowedSortFields.put("downloadCount", "z.download_count");
        allowedSortFields.put("fileSize", "z.file_size");
        allowedSortFields.put("totalPages", "z.total_pages");

        String sortColumn = allowedSortFields.getOrDefault(sortField, "z.updated_at");
        String sortType = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<ZineResponse> pageResult = zineMapper.getZineListWithAdvancedFilters(criteria, offset, limit, sortColumn, sortType);
        int totalCount = zineMapper.countZinesWithAdvancedFilters(criteria);

        PageDataResponse<ZineResponse> data = new PageDataResponse<>(page, limit, totalCount, pageResult);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DataResponse<Zine> update(Long id, Zine zine, MultipartFile file) throws IOException {
        Zine existingZine = zineMapper.findById(id);
        if (existingZine == null) throw new DataNotFoundException();

        zine.setId(id);

        if (file != null && !file.isEmpty()) {
            Path oldFilePath = Paths.get(existingZine.getFilePath());
            if (Files.exists(oldFilePath)) Files.delete(oldFilePath);
            Path savedPath = fileUtil.saveFile(file, "uploads/zines", id);
            zine.setFilePath(savedPath.toString());
        } else {
            zine.setFilePath(existingZine.getFilePath());
        }

        zineMapper.updateZine(zine);
        Zine data = zineMapper.findById(id);
        if (data != null) return new DataResponse<>(SUCCESS, ResponseMessage.DATA_UPDATED, HttpStatus.OK.value(), data);
        throw new DataNotFoundException();
    }

    @Override
    public DefaultResponse delete(Long id) throws IOException {
        Zine zine = zineMapper.findById(id);
        if (zine != null) {
            if (zine.getFilePath() != null) {
                Path filePath = Paths.get(zine.getFilePath());
                if (Files.exists(filePath)) Files.delete(filePath);
            }
            zineMapper.deleteZine(id);
            return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());
        }
        throw new DataNotFoundException();
    }

    @Override
    @Transactional
    public ResponseEntity<?> getDownloadUrl(String slug, HttpServletRequest request) {
        try {
            Zine zine = zineMapper.findZineBySlug(slug);
            if (zine == null) throw new DataNotFoundException();

            String fileUrl = zine.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) throw new DataNotFoundException();

            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);

            boolean hasDownloaded = userId != null
                    ? zineMapper.hasActionByUserAndZine(zine.getId(), userId, "download")
                    : zineMapper.hasActionByHash(viewerHash, "download");

            if (!hasDownloaded) {
                try {
                    zineMapper.insertAction(ZineView.builder()
                            .zineId(zine.getId()).slug(slug).userId(userId)
                            .ipAddress(ipAddress).userAgent(userAgent)
                            .viewerHash(viewerHash).actionType("download")
                            .build());
                    zineMapper.incrementDownloadCount(zine.getId());
                } catch (DuplicateKeyException e) {
                    log.warn("Race condition on download insert for slug: {} — skipping", slug);
                }
            }

            String username = headerHolder.getUsername();
            if (username != null && !username.isEmpty()) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    Map<String, Object> activityMeta = Map.of(
                            "action", "download_zine",
                            "zine_title", zine.getTitle(),
                            "zine_slug", zine.getSlug(),
                            "is_unique_download", !hasDownloaded,
                            "user_type", "authenticated",
                            "device_info", Map.of(
                                    "type", headerHolder.getDeviceType(),
                                    "browser", headerHolder.getBrowser(),
                                    "os", headerHolder.getOs(),
                                    "ip", ipAddress
                            )
                    );
                    UserActivity activity = new UserActivity();
                    activity.setUserId(user.getId());
                    activity.setActivityType("download");
                    activity.setEntityType("ZINE");
                    activity.setEntityId(zine.getId());
                    activity.setMetadata(new ObjectMapper().writeValueAsString(activityMeta));
                    activity.setCreatedAt(LocalDateTime.now());
                    userMapper.insertUserActivity(activity);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "downloadUrl", fileUrl,
                    "filename", fileUtil.sanitizeFilename(zine.getTitle()) + ".epub"
            ));

        } catch (DataNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting download URL for zine: {}", slug, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public DataResponse<List<GenreResponse>> getAllGenres(boolean includeZineCount) {
        List<Genre> genres = includeZineCount
                ? genreMapper.findAllWithBookCount()
                : genreMapper.findAll();

        List<GenreResponse> responses = genres.stream().map(this::mapToGenreResponse).toList();
        return new DataResponse<>(SUCCESS, "Genres retrieved successfully", HttpStatus.OK.value(), responses);
    }

    @Override
    public DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy) {
        Map<String, String> sortFieldMap = Map.of(
                "name", "name",
                "bookCount", "total_books",
                "createdAt", "created_at");
        String sortColumn = sortFieldMap.getOrDefault(sortBy, "name");
        int offset = (page - 1) * limit;

        List<Author> authors = authorMapper.findAllWithPagination(offset, limit, search, sortColumn);
        List<AuthorResponse> responses = authors.stream().map(this::mapToAuthorResponse).toList();
        int total = authorMapper.countAll(search);

        return new DatatableResponse<>(SUCCESS, "Authors retrieved successfully",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, responses));
    }

    @Override
    public DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search) {
        int offset = (page - 1) * limit;
        List<Contributor> contributors = contributorMapper.findAllWithPagination(offset, limit, role, search);
        List<ContributorResponse> responses = contributors.stream().map(this::mapToContributorResponse).toList();
        int total = contributorMapper.countAll(role, search);

        return new DatatableResponse<>(SUCCESS, "Contributors retrieved successfully",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, responses));
    }

    private int calculateVolume(String collectionName, Integer publicationYear) {
        if (collectionName == null || collectionName.isEmpty() || publicationYear == null) {
            return 1;
        }
        try {
            Integer earliestYear = zineMapper.findEarliestPublicationYearByCollection(collectionName);
            if (earliestYear == null) {
                log.info("No existing zine in collection '{}', volume = 1", collectionName);
                return 1;
            }
            int volume = (publicationYear - earliestYear) + 1;
            if (volume < 1) volume = 1;
            log.info("Collection '{}': earliest={}, current={}, volume={}",
                    collectionName, earliestYear, publicationYear, volume);
            return volume;
        } catch (Exception e) {
            log.warn("Failed to calculate volume for '{}': {}", collectionName, e.getMessage());
            return 1;
        }
    }

    private Book toBookAdapter(Zine zine) {
        Book adapter = new Book();
        adapter.setId(zine.getId());
        adapter.setTitle(zine.getTitle());
        adapter.setSlug(zine.getSlug());
        adapter.setFileUrl(zine.getFileUrl());
        adapter.setCoverImageUrl(zine.getCoverImageUrl());
        adapter.setCategory("ZINE:" + (zine.getCategory() != null ? zine.getCategory() : ""));
        return adapter;
    }

    private Zine checkExistingZineWithSameAuthor(String slug, CompleteEpubMetadata epubMeta) {
        Zine existingZine = zineMapper.findBySlug(slug);
        if (existingZine == null) return null;
        if (epubMeta.getAuthors() == null || epubMeta.getAuthors().isEmpty()) return null;

        List<Author> existingAuthors = zineMapper.findAuthorsByZineId(existingZine.getId());
        if (existingAuthors.isEmpty()) return null;

        for (AuthorMetadata authorMeta : epubMeta.getAuthors()) {
            String authorSlug = fileUtil.sanitizeFilename(authorMeta.getName());
            for (Author existingAuthor : existingAuthors) {
                if (existingAuthor.getSlug().equalsIgnoreCase(authorSlug)) {
                    return existingZine;
                }
            }
        }
        return null;
    }

    private void genreProcessing(CompleteEpubMetadata epubMeta, Zine zine) {
        if (epubMeta.getSubjects() == null || epubMeta.getSubjects().isEmpty()) return;

        String[] palette = {"#6B7280", "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6"};

        for (String subject : epubMeta.getSubjects()) {
            Genre genre = genreMapper.findByName(subject);

            if (genre == null) {
                genre = new Genre();
                genre.setName(subject);
                genre.setSlug(fileUtil.sanitizeFilename(subject));
                genre.setDescription("Auto-generated from EPUB metadata");
                genre.setColorHex(palette[Math.abs(subject.hashCode()) % palette.length]);
                genre.setIconName("book");
                genre.setIsFiction(false);
                genre.setCreatedAt(Instant.now());
                genreMapper.insertGenre(genre);
            }

            zineMapper.insertZineGenre(zine.getId(), genre.getId());
        }
    }

    private void authorProcessing(CompleteEpubMetadata epubMeta, Zine zine) {
        if (epubMeta.getAuthors() == null || epubMeta.getAuthors().isEmpty()) return;

        List<Author> existingAuthors = zineMapper.findAuthorsByZineId(zine.getId());
        Set<Long> existingAuthorIds = existingAuthors.stream()
                .map(Author::getId).collect(Collectors.toSet());

        for (AuthorMetadata authorMeta : epubMeta.getAuthors()) {
            String slug = fileUtil.sanitizeFilename(authorMeta.getName());
            Author author = authorMapper.findAuthorBySlug(slug);

            if (author != null) {
                boolean needsUpdate = false;

                if (authorMeta.getBirthDate() != null && !authorMeta.getBirthDate().equals(author.getBirthDate())) {
                    author.setBirthDate(authorMeta.getBirthDate());
                    needsUpdate = true;
                }
                if (authorMeta.getDeathDate() != null && !authorMeta.getDeathDate().equals(author.getDeathDate())) {
                    author.setDeathDate(authorMeta.getDeathDate());
                    needsUpdate = true;
                }
                if (authorMeta.getBiography() != null && !authorMeta.getBiography().equals(author.getBiography())) {
                    author.setBiography(authorMeta.getBiography());
                    needsUpdate = true;
                }

                if (!existingAuthorIds.contains(author.getId())) {
                    author.setTotalBooks(author.getTotalBooks() + 1);
                    zineMapper.insertZineAuthor(zine.getId(), author.getId());
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    author.setUpdatedAt(LocalDateTime.now());
                    authorMapper.updateAuthor(author);
                }
            } else {
                Author newAuthor = new Author();
                newAuthor.setName(authorMeta.getName());
                newAuthor.setSlug(slug);
                newAuthor.setBirthDate(authorMeta.getBirthDate());
                newAuthor.setDeathDate(authorMeta.getDeathDate());
                newAuthor.setBirthPlace(authorMeta.getBirthPlace());
                newAuthor.setNationality(authorMeta.getNationality());
                newAuthor.setBiography(authorMeta.getBiography());
                newAuthor.setPhotoUrl(authorMeta.getPhotoUrl());
                newAuthor.setTotalBooks(1);
                newAuthor.setCreatedAt(LocalDateTime.now());
                newAuthor.setUpdatedAt(LocalDateTime.now());
                authorMapper.insertAuthor(newAuthor);
                zineMapper.insertZineAuthor(zine.getId(), newAuthor.getId());
            }
        }
    }

    private void contributorProcessing(CompleteEpubMetadata epubMeta, Zine zine) {
        for (ContributorMetadata contribMeta : epubMeta.getContributors()) {
            Contributor contributor = contributorMapper.findByNameAndRole(contribMeta.getName(), contribMeta.getRole());
            if (contributor == null) {
                contributor = new Contributor();
                contributor.setName(contribMeta.getName());
                contributor.setRole(contribMeta.getRole());
                contributor.setWebsiteUrl(null);
                contributor.setCreatedAt(LocalDateTime.now());
                contributor.setUpdatedAt(LocalDateTime.now());

                String baseSlug = fileUtil.sanitizeFilename(contribMeta.getName());
                String finalSlug = baseSlug;
                if (contributorMapper.findBySlug(finalSlug) != null) {
                    finalSlug = baseSlug + "-" + contribMeta.getRole().toLowerCase().replace(" ", "-");
                }
                contributor.setSlug(finalSlug);
                contributorMapper.insertContributor(contributor);
            }
            zineMapper.insertZineContributor(zine.getId(), contributor.getId(), contribMeta.getRole());
        }
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

    private GenreResponse mapToGenreResponse(Genre genre) {
        GenreResponse r = new GenreResponse();
        r.setId(genre.getId());
        r.setName(genre.getName());
        r.setSlug(genre.getSlug());
        r.setDescription(genre.getDescription());
        r.setBookCount(genre.getBookCount() != null ? genre.getBookCount() : 0);
        r.setCreatedAt(genre.getCreatedAt());
        return r;
    }

    private AuthorResponse mapToAuthorResponse(Author author) {
        AuthorResponse r = new AuthorResponse();
        r.setId(author.getId());
        r.setName(author.getName());
        r.setSlug(author.getSlug());
        r.setBirthDate(author.getBirthDate());
        r.setDeathDate(author.getDeathDate());
        r.setBirthPlace(author.getBirthPlace());
        r.setNationality(author.getNationality());
        r.setBiography(author.getBiography());
        r.setPhotoUrl(author.getPhotoUrl());
        r.setTotalBooks(author.getTotalBooks());
        r.setCreatedAt(author.getCreatedAt());
        r.setUpdatedAt(author.getUpdatedAt());
        return r;
    }

    private ContributorResponse mapToContributorResponse(Contributor contributor) {
        ContributorResponse r = new ContributorResponse();
        r.setId(contributor.getId());
        r.setName(contributor.getName());
        r.setSlug(contributor.getSlug());
        r.setRole(contributor.getRole());
        r.setWebsiteUrl(contributor.getWebsiteUrl());
        r.setTotalBooks(contributor.getTotalBooks() != null ? contributor.getTotalBooks() : 0);
        r.setCreatedAt(contributor.getCreatedAt());
        r.setUpdatedAt(contributor.getUpdatedAt());
        return r;
    }
}