package com.masasilam.app.service.book.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masasilam.app.config.ClearChapterCache;
import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.ForbiddenException;
import com.masasilam.app.exception.custom.InternalServerErrorException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.author.AuthorMapper;
import com.masasilam.app.mapper.author.ContributorMapper;
import com.masasilam.app.mapper.book.*;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.*;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.*;
import com.masasilam.app.repository.ChapterRepository;
import com.masasilam.app.service.book.BookService;
import com.masasilam.app.service.book.EpubService;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookMapper bookMapper;
    private final AuthorMapper authorMapper;
    private final ContributorMapper contributorMapper;
    private final LanguageMapper languageMapper;
    private final CopyrightStatusMapper copyrightStatusMapper;
    private final UserMapper userMapper;
    private final GenreMapper genreMapper;
    private final BookSeriesMapper bookSeriesMapper;
    private final HeaderHolder headerHolder;
    private final EpubService epubService;
    private final ChapterRepository bookChapterRepository;
    private final FileUtil fileUtil;
    private static final String SUCCESS = "Success";
    private static final String DOWNLOAD = "Download";

    @Value("${file.upload.max-size:52428800}")
    private String maxFileSizeStr;

    @Transactional
    @ClearChapterCache
    public DataResponse<BookResponse> createBook(BookRequest request) {
        try {
            if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty()) {
                throw new UnauthorizedException();
            }

            if (headerHolder.getRoles() == null || !Arrays.asList(headerHolder.getRoles()).contains("ADMIN")) {
                throw new ForbiddenException();
            }

            long maxSizeBytes = fileUtil.parseFileSize(maxFileSizeStr);
            fileUtil.validateFile(request.getBookFile(), maxSizeBytes);

            String fileExtension = fileUtil.getFileExtension(request.getBookFile().getOriginalFilename());
            if (!"epub".equalsIgnoreCase(fileExtension)) {
                throw new IllegalArgumentException("Only EPUB files are supported for auto-metadata extraction");
            }

            log.info("Extracting complete metadata from EPUB file");

            CompleteEpubMetadata epubMeta = EpubMetadataExtractor.extractCompleteMetadata(request.getBookFile().getInputStream());

            String finalTitle = epubMeta.getTitle();
            String finalSubtitle = epubMeta.getSubtitle();
            String finalDescription = epubMeta.getDescription();
            String finalPublisher = epubMeta.getPublisher();
            String finalCategory = epubMeta.getCategory();

            if (finalTitle == null || finalTitle.isEmpty()) {
                throw new IllegalArgumentException("Title not found in EPUB metadata.");
            }
            if (finalPublisher == null || finalPublisher.isEmpty()) {
                throw new IllegalArgumentException("Publisher not found in EPUB metadata.");
            }
            if (epubMeta.getPublicationYear() == null) {
                throw new IllegalArgumentException("Publication year not found in EPUB metadata.");
            }

            String baseSlug = fileUtil.sanitizeFilename(finalTitle);
            Book existingBook = checkExistingBookWithSameAuthor(baseSlug, epubMeta);

            if (existingBook != null) {
                log.info("Found existing book with same slug '{}' and author(s). Updating instead of creating new.", baseSlug);
                return updateExistingBook(existingBook, request.getBookFile(), epubMeta);
            }

            String finalSlug = baseSlug;
            int duplicateCount = bookMapper.countBySlug(finalSlug);
            if (duplicateCount > 0) {
                String authorSuffix = "";
                if (epubMeta.getAuthors() != null && !epubMeta.getAuthors().isEmpty()) {
                    String authorName = epubMeta.getAuthors().getFirst().getName();
                    String[] nameParts = authorName.trim().split("\\s+");
                    authorSuffix = fileUtil.sanitizeFilename(nameParts[nameParts.length - 1]);
                }

                if (!authorSuffix.isEmpty()) {
                    finalSlug = baseSlug + "-" + authorSuffix;
                    if (bookMapper.countBySlug(finalSlug) > 0) {
                        finalSlug = baseSlug + "-" + authorSuffix + "-" + System.currentTimeMillis();
                        log.warn("Slug '{}-{}' juga sudah ada, pakai fallback: {}", baseSlug, authorSuffix, finalSlug);
                    }
                } else {
                    finalSlug = baseSlug + "-" + System.currentTimeMillis();
                }
                log.info("Slug '{}' already exists, using unique slug: {}", baseSlug, finalSlug);
            }

            Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
            CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

            Long seriesId = getOrCreateSeries(epubMeta);

            FileStorageResult bookResult = fileUtil.saveAndUploadBookFile(request.getBookFile(), finalTitle);
            BookMetadata metadata = fileUtil.extractBookMetadata(request.getBookFile());

            Book book = new Book();
            book.setTitle(finalTitle);
            book.setSlug(finalSlug);
            book.setSubtitle(finalSubtitle);
            book.setSeriesId(seriesId);
            book.setSeriesOrder(epubMeta.getSeriesOrder());
            book.setEdition(1);
            book.setPublicationYear(epubMeta.getPublicationYear());
            book.setPublisher(finalPublisher);
            book.setLanguageId(language.getId());
            book.setDescription(finalDescription);
            book.setFileUrl(bookResult.getCloudUrl());
            book.setSource(epubMeta.getSource());
            book.setFileFormat(metadata.getFileFormat());
            book.setFileSize(metadata.getFileSize());
            book.setCopyrightStatusId(copyrightStatus.getId());
            book.setViewCount(0);
            book.setReadCount(0);
            book.setDownloadCount(0);
            book.setIsActive(true);
            book.setIsFeatured(false);
            book.setPublishedAt(epubMeta.getPublishedAt() != null ? epubMeta.getPublishedAt().atStartOfDay() : null);
            book.setCategory(finalCategory);
            book.setCreatedAt(epubMeta.getUpdatedAt());
            book.setUpdatedAt(epubMeta.getUpdatedAt());
            book.setFirstPublished(epubMeta.getFirstPublished());
            book.setFirstPublisher(epubMeta.getFirstPublisher());

            bookMapper.insertBook(book);
            log.info("Book created with ID: {} and slug: {}", book.getId(), book.getSlug());

            EpubProcessResult result = epubService.processEpubFile(
                    request.getBookFile(), book, bookChapterRepository);
            log.info("EPUB processed: {} chapters, {} words", result.getTotalChapters(), result.getTotalWords());

            book.setTotalWord(result.getTotalWords());
            book.setTotalPages(result.getTotalChapters());
            book.setEstimatedReadTime(fileUtil.calculateEstimatedReadTime(result.getTotalWords()));
            book.setCoverImageUrl(result.getCoverImageUrl());
            book.setFileUrlArchive(book.getFileUrl());
            bookMapper.updateBook(book);

            genreProcessing(epubMeta, book);
            authorProcessing(epubMeta, book);
            contributorProcessing(epubMeta, book);

            BookResponse data = bookMapper.getBookDetailBySlug(book.getSlug());
            log.info("Book successfully created with full automation: {}", finalTitle);
            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, 201, data);

        } catch (Exception e) {
            log.error("Error creating book: {}", e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }

    private Long getOrCreateSeries(CompleteEpubMetadata epubMeta) {
        if (epubMeta.getSeriesName() == null || epubMeta.getSeriesName().isBlank()) {
            log.info("No series metadata found in EPUB, skipping series processing");
            return null;
        }

        String slug = fileUtil.sanitizeFilename(epubMeta.getSeriesName());

        BookSeries existing = bookSeriesMapper.findBySlug(slug);
        if (existing != null) {
            if (existing.getDescription() == null && epubMeta.getSeriesDescription() != null) {
                existing.setDescription(epubMeta.getSeriesDescription());
                bookSeriesMapper.updateSeries(existing);
                log.info("Updated series description for '{}' (ID: {})", existing.getName(), existing.getId());
            }
            log.info("Found existing series '{}' (ID: {})", existing.getName(), existing.getId());
            return existing.getId();
        }

        BookSeries series = new BookSeries();
        series.setName(epubMeta.getSeriesName());
        series.setSlug(slug);
        series.setDescription(epubMeta.getSeriesDescription());
        series.setCoverImageUrl(null);
        series.setCreatedAt(Instant.now());

        bookSeriesMapper.insertSeries(series);
        log.info("Auto-created book_series: '{}' (ID: {})", series.getName(), series.getId());
        return series.getId();
    }

    private Book checkExistingBookWithSameAuthor(String slug, CompleteEpubMetadata epubMeta) {
        Book existingBook = bookMapper.findBySlug(slug);
        if (existingBook == null) return null;

        if (epubMeta.getAuthors() == null || epubMeta.getAuthors().isEmpty()) return null;

        List<Author> existingAuthors = bookMapper.findAuthorsByBookId(existingBook.getId());
        if (existingAuthors.isEmpty()) return null;

        boolean authorMatches = false;
        for (AuthorMetadata authorMeta : epubMeta.getAuthors()) {
            String authorSlug = fileUtil.sanitizeFilename(authorMeta.getName());
            for (Author existingAuthor : existingAuthors) {
                if (existingAuthor.getSlug().equalsIgnoreCase(authorSlug)) {
                    authorMatches = true;
                    log.info("Found matching author by slug: {} = {}", existingAuthor.getSlug(), authorSlug);
                    break;
                }
            }
            if (authorMatches) break;
        }

        if (authorMatches) {
            log.info("Found existing book '{}' with matching author(s). Will update instead of creating new.",
                    existingBook.getTitle());
        }
        return authorMatches ? existingBook : null;
    }

    @Transactional
    @ClearChapterCache
    public DataResponse<BookResponse> updateExistingBook(
            Book existingBook, MultipartFile newFile, CompleteEpubMetadata epubMeta) throws IOException {

        log.info("Updating existing book ID: {} - {}", existingBook.getId(), existingBook.getTitle());

        if (existingBook.getFileUrl() != null) {
            fileUtil.deleteFile(existingBook.getFileUrl());
            log.info("Deleted old book file: {}", existingBook.getFileUrl());
        }
        if (existingBook.getCoverImageUrl() != null) {
            fileUtil.deleteFile(existingBook.getCoverImageUrl());
            log.info("Deleted old cover image: {}", existingBook.getCoverImageUrl());
        }

        bookMapper.deleteBookGenres(existingBook.getId());
        bookMapper.deleteBookContributors(existingBook.getId());
        log.info("Deleted old relationships for book ID: {}", existingBook.getId());

        FileStorageResult bookResult = fileUtil.saveAndUploadBookFile(newFile, existingBook.getTitle());
        BookMetadata metadata = fileUtil.extractBookMetadata(newFile);

        Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
        CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

        Long seriesId = getOrCreateSeries(epubMeta);

        existingBook.setTitle(epubMeta.getTitle());
        existingBook.setSubtitle(epubMeta.getSubtitle());
        existingBook.setSeriesId(seriesId);
        existingBook.setSeriesOrder(epubMeta.getSeriesOrder());
        existingBook.setPublicationYear(epubMeta.getPublicationYear());
        existingBook.setPublisher(epubMeta.getPublisher());
        existingBook.setLanguageId(language.getId());
        existingBook.setDescription(epubMeta.getDescription());
        existingBook.setFileUrl(bookResult.getCloudUrl());
        existingBook.setSource(epubMeta.getSource());
        existingBook.setFileFormat(metadata.getFileFormat());
        existingBook.setFileSize(metadata.getFileSize());
        existingBook.setCopyrightStatusId(copyrightStatus.getId());
        existingBook.setPublishedAt(epubMeta.getPublishedAt() != null ? epubMeta.getPublishedAt().atStartOfDay() : null);
        existingBook.setCategory(epubMeta.getCategory());
        existingBook.setUpdatedAt(epubMeta.getUpdatedAt());
        existingBook.setFirstPublished(epubMeta.getFirstPublished());
        existingBook.setFirstPublisher(epubMeta.getFirstPublisher());
        existingBook.setEdition(existingBook.getEdition() == null ? 1 : Math.min(existingBook.getEdition() + 1, 2));

        EpubProcessResult result = epubService.processEpubFileForUpdate(newFile, existingBook, bookChapterRepository);
        log.info("New EPUB processed: {} chapters, {} words", result.getTotalChapters(), result.getTotalWords());

        existingBook.setTotalWord(result.getTotalWords());
        existingBook.setTotalPages(result.getTotalChapters());
        existingBook.setEstimatedReadTime(fileUtil.calculateEstimatedReadTime(result.getTotalWords()));
        existingBook.setCoverImageUrl(result.getCoverImageUrl());

        bookMapper.updateBook(existingBook);
        log.info("Updated book entity ID: {}", existingBook.getId());

        genreProcessing(epubMeta, existingBook);
        authorProcessing(epubMeta, existingBook);
        contributorProcessing(epubMeta, existingBook);

        BookResponse data = bookMapper.getBookDetailBySlug(existingBook.getSlug());
        log.info("Book successfully updated: {}", existingBook.getTitle());
        return new DataResponse<>(SUCCESS, "Book updated successfully", 200, data);
    }

    private void genreProcessing(CompleteEpubMetadata epubMeta, Book book) {
        if (epubMeta.getSubjects() != null && !epubMeta.getSubjects().isEmpty()) {
            for (String subject : epubMeta.getSubjects()) {
                Genre genre = genreMapper.findByName(subject);
                if (genre == null) {
                    genre = new Genre();
                    genre.setName(subject);
                    genre.setSlug(fileUtil.sanitizeFilename(subject));
                    genre.setDescription("Auto-generated from EPUB metadata");
                    genre.setColorHex("#6B7280");
                    genre.setIconName("book");
                    genre.setIsFiction(false);
                    genre.setIsActive(true);
                    genre.setCreatedAt(Instant.now());
                    genreMapper.insertGenre(genre);
                    log.info("Auto-created genre: {}", subject);
                }
                bookMapper.insertBookGenre(book.getId(), genre.getId());
            }
        }
    }

    private void authorProcessing(CompleteEpubMetadata epubMeta, Book book) {
        if (epubMeta.getAuthors() == null || epubMeta.getAuthors().isEmpty()) return;

        List<Author> existingAuthors = bookMapper.findAuthorsByBookId(book.getId());
        Set<Long> existingAuthorIds = existingAuthors.stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        for (AuthorMetadata authorMeta : epubMeta.getAuthors()) {
            String slug = fileUtil.sanitizeFilename(authorMeta.getName());
            Author author = authorMapper.findAuthorBySlug(slug);

            if (author != null) {
                boolean needsUpdate = false;

                if (authorMeta.getBirthDate() != null
                        && !authorMeta.getBirthDate().equals(author.getBirthDate())) {
                    author.setBirthDate(authorMeta.getBirthDate());
                    needsUpdate = true;
                }
                if (authorMeta.getDeathDate() != null
                        && !authorMeta.getDeathDate().equals(author.getDeathDate())) {
                    author.setDeathDate(authorMeta.getDeathDate());
                    needsUpdate = true;
                }
                if (authorMeta.getBirthPlace() != null
                        && !authorMeta.getBirthPlace().equals(author.getBirthPlace())) {
                    author.setBirthPlace(authorMeta.getBirthPlace());
                    needsUpdate = true;
                }
                if (authorMeta.getDeathPlace() != null
                        && !authorMeta.getDeathPlace().equals(author.getDeathPlace())) {
                    author.setDeathPlace(authorMeta.getDeathPlace());
                    needsUpdate = true;
                }
                if (authorMeta.getNationality() != null
                        && !authorMeta.getNationality().equals(author.getNationality())) {
                    author.setNationality(authorMeta.getNationality());
                    needsUpdate = true;
                }
                if (authorMeta.getBiography() != null
                        && !authorMeta.getBiography().equals(author.getBiography())) {
                    author.setBiography(authorMeta.getBiography());
                    needsUpdate = true;
                }
                if (authorMeta.getPhotoUrl() != null
                        && !authorMeta.getPhotoUrl().equals(author.getPhotoUrl())) {
                    author.setPhotoUrl(authorMeta.getPhotoUrl());
                    needsUpdate = true;
                }

                if (!existingAuthorIds.contains(author.getId())) {
                    author.setTotalBooks(author.getTotalBooks() + 1);
                    bookMapper.insertBookAuthor(book.getId(), author.getId());
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    author.setUpdatedAt(LocalDateTime.now());
                    authorMapper.updateAuthor(author);
                    log.info("Updated author '{}' with new metadata", author.getName());
                }

            } else {
                Author newAuthor = new Author();
                newAuthor.setName(authorMeta.getName());
                newAuthor.setSlug(slug);
                newAuthor.setBirthDate(authorMeta.getBirthDate());
                newAuthor.setDeathDate(authorMeta.getDeathDate());
                newAuthor.setBirthPlace(authorMeta.getBirthPlace());
                newAuthor.setDeathPlace(authorMeta.getDeathPlace());
                newAuthor.setNationality(authorMeta.getNationality());
                newAuthor.setBiography(authorMeta.getBiography());
                newAuthor.setPhotoUrl(authorMeta.getPhotoUrl());
                newAuthor.setTotalBooks(1);
                newAuthor.setCreatedAt(LocalDateTime.now());
                newAuthor.setUpdatedAt(LocalDateTime.now());

                authorMapper.insertAuthor(newAuthor);
                bookMapper.insertBookAuthor(book.getId(), newAuthor.getId());
                log.info("Auto-created author: {} with complete metadata", newAuthor.getName());
            }
        }
    }

    private void contributorProcessing(CompleteEpubMetadata epubMeta, Book book) {
        for (ContributorMetadata contribMeta : epubMeta.getContributors()) {
            log.info("Processing contributor from EPUB: name='{}', role='{}'", contribMeta.getName(), contribMeta.getRole());

            Contributor contributor = contributorMapper.findByNameAndRole(contribMeta.getName(), contribMeta.getRole());
            if (contributor == null) {
                log.info("Contributor not found in DB, creating new: {} ({})", contribMeta.getName(), contribMeta.getRole());

                contributor = new Contributor();
                contributor.setName(contribMeta.getName());
                contributor.setRole(contribMeta.getRole());
                contributor.setWebsiteUrl(null);
                contributor.setCreatedAt(LocalDateTime.now());
                contributor.setUpdatedAt(LocalDateTime.now());

                String contribBaseSlug = fileUtil.sanitizeFilename(contribMeta.getName());
                String contribFinalSlug = contribBaseSlug;

                Contributor existingBySlug = contributorMapper.findBySlug(contribFinalSlug);
                if (existingBySlug != null) {
                    contribFinalSlug = contribBaseSlug + "-" + contribMeta.getRole().toLowerCase().replace(" ", "-");
                    log.info("Slug collision detected, using: {}", contribFinalSlug);
                }
                contributor.setSlug(contribFinalSlug);
                contributorMapper.insertContributor(contributor);

                log.info("Auto-created contributor: {} ({}) with slug: {}", contributor.getName(), contributor.getRole(), contributor.getSlug());
            } else {
                log.info("Using existing contributor from DB: {} (ID: {})", contributor.getName(), contributor.getId());
            }

            bookMapper.insertBookContributor(book.getId(), contributor.getId(), contribMeta.getRole());
            log.info("Inserted book_contributor: bookId={}, contributorId={}, role={}", book.getId(), contributor.getId(), contribMeta.getRole());
        }
    }

    @Override
    @Transactional
    public DataResponse<BookResponse> getBookDetailBySlug(String slug, HttpServletRequest request) throws NoSuchAlgorithmException {
        try {
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String userType = userId != null ? "authenticated (userId: " + userId + ")" : "guest";
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);

            log.info("Checking view for slug: {}, User: {}, IP: {}, Hash: {}", slug, userType, ipAddress, viewerHash);

            Long bookId = bookMapper.getBookIdBySlug(slug);
            if (bookId == null) throw new DataNotFoundException();

            boolean hasViewed = userId != null ? bookMapper.hasActionByUserAndBook(bookId, userId, "view") : bookMapper.hasActionByHash(viewerHash, "view");

            if (!hasViewed) {
                try {
                    bookMapper.insertAction(BookView.builder()
                            .bookId(bookId).slug(slug).userId(userId)
                            .ipAddress(ipAddress).userAgent(userAgent)
                            .viewerHash(viewerHash).actionType("view")
                            .build());
                    bookMapper.incrementViewCountBySlug(slug);
                    log.info("✓ New view recorded for slug: {} by {}", slug, userType);
                } catch (DuplicateKeyException e) {
                    log.warn("Race condition on view insert for slug: {} by {} — skipping", slug, userType);
                }
            } else {
                log.info("✗ Duplicate view detected for slug: {} by {} - NOT incrementing", slug, userType);
            }

            BookResponse data = bookMapper.getBookDetailBySlug(slug);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
            }
            throw new DataNotFoundException();

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error when get book detail for slug: {}", slug, e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder, BookSearchCriteria criteria) {
        try {
            Map<String, String> allowedSortFields = new HashMap<>();
            allowedSortFields.put("updateAt", "b.updated_at");
            allowedSortFields.put("title", "b.title");
            allowedSortFields.put("publishedAt", "b.published_at");
            allowedSortFields.put("estimatedReadTime", "b.estimated_read_time");
            allowedSortFields.put("totalWord", "b.total_word");
            allowedSortFields.put("averageRating", "average_rating");
            allowedSortFields.put("viewCount", "b.view_count");
            allowedSortFields.put("readCount", "b.read_count");
            allowedSortFields.put("downloadCount", "b.download_count");
            allowedSortFields.put("fileSize", "b.file_size");
            allowedSortFields.put("totalPages", "b.total_pages");

            String sortColumn = allowedSortFields.getOrDefault(sortField, "b.updated_at");
            String sortType = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

            int offset = (page - 1) * limit;

            log.info("Fetching books with criteria: {}", criteria);
            log.info("Sort by: {} {}, Page: {}, Limit: {}", sortColumn, sortType, page, limit);

            List<BookResponse> pageResult = bookMapper.getBookListWithAdvancedFilters(criteria, offset, limit, sortColumn, sortType);

            int totalCount = bookMapper.countBooksWithAdvancedFilters(criteria);

            log.info("Found {} books, returning page {} with {} items", totalCount, page, pageResult.size());

            PageDataResponse<BookResponse> data = new PageDataResponse<>(page, limit, totalCount, pageResult);

            return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);

        } catch (Exception e) {
            log.error("Error fetching paginated books with advanced filters", e);
            throw e;
        }
    }

    @Override
    public DataResponse<Book> update(Long id, Book book, MultipartFile file) throws IOException {
        try {
            Book existingEbook = bookMapper.getDetailEbook(id);
            if (existingEbook == null) {
                throw new DataNotFoundException();
            }

            book.setId(id);

            if (file != null && !file.isEmpty()) {
                Path oldFilePath = Paths.get(existingEbook.getFilePath());
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                }

                Path savedFilePath = fileUtil.saveFile(file, "uploads", id);
                book.setFilePath(savedFilePath.toString());
            } else {
                book.setFilePath(existingEbook.getFilePath());
            }

            bookMapper.updateBook(book);
            Book data = bookMapper.getDetailEbook(id);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_UPDATED, HttpStatus.OK.value(), data);
            } else {
                throw new DataNotFoundException();
            }

        } catch (Exception e) {
            log.error("Error when update ebook", e);
            throw e;
        }
    }

    @Override
    public DefaultResponse delete(Long id) throws IOException {
        try {
            Book ebook = bookMapper.getDetailEbook(id);
            if (ebook != null) {
                Path filePath = Paths.get(ebook.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                bookMapper.deleteEbook(id);
                return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());
            } else {
                throw new DataNotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when delete ebook", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> getDownloadUrl(String slug, HttpServletRequest request) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) throw new DataNotFoundException();

            String fileUrl = book.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) throw new DataNotFoundException();

            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);

            boolean hasDownloaded = userId != null ? bookMapper.hasActionByUserAndBook(book.getId(), userId, DOWNLOAD) : bookMapper.hasActionByHash(viewerHash, DOWNLOAD);

            if (!hasDownloaded) {
                try {
                    bookMapper.insertAction(BookView.builder()
                            .bookId(book.getId()).slug(slug).userId(userId)
                            .ipAddress(ipAddress).userAgent(userAgent)
                            .viewerHash(viewerHash).actionType(DOWNLOAD)
                            .build());
                    bookMapper.incrementDownloadCount(book.getId());
                } catch (DuplicateKeyException e) {
                    log.warn("Race condition on download insert for slug: {} — skipping", slug);
                }
            }

            String username = headerHolder.getUsername();
            if (username != null && !username.isEmpty()) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    Map<String, Object> metadata = Map.of(
                            "action", "download_book",
                            "book_title", book.getTitle(),
                            "book_slug", book.getSlug(),
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
                    activity.setActivityType(DOWNLOAD);
                    activity.setEntityType("BOOK");
                    activity.setEntityId(book.getId());
                    activity.setMetadata(new ObjectMapper().writeValueAsString(metadata));
                    activity.setCreatedAt(LocalDateTime.now());
                    userMapper.insertUserActivity(activity);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "downloadUrl", fileUrl,
                    "filename", book.getTitle() != null
                            ? fileUtil.sanitizeFilename(book.getTitle()) + ".epub"
                            : slug + ".epub"
            ));

        } catch (DataNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting download URL: {}", slug, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public DataResponse<List<GenreResponse>> getAllGenres(boolean includeBookCount) {
        try {
            List<Genre> genres;

            if (includeBookCount) {
                genres = genreMapper.findAllWithBookCount();
            } else {
                genres = genreMapper.findAll();
            }

            List<GenreResponse> responses = genres.stream().map(this::mapToGenreResponse).toList();

            log.info("Retrieved {} genres", responses.size());

            return new DataResponse<>(SUCCESS, "Genres retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting all genres", e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy) {
        try {
            Map<String, String> sortFieldMap = new HashMap<>();
            sortFieldMap.put("name", "name");
            sortFieldMap.put("bookCount", "total_books");
            sortFieldMap.put("createdAt", "created_at");

            String sortColumn = sortFieldMap.getOrDefault(sortBy, "name");

            int offset = (page - 1) * limit;
            List<Author> authors = authorMapper.findAllWithPagination(offset, limit, search, sortColumn);

            List<AuthorResponse> responses = authors.stream().map(this::mapToAuthorResponse).toList();

            int totalAuthors = authorMapper.countAll(search);
            int totalPages = (int) Math.ceil((double) totalAuthors / limit);

            PageDataResponse<AuthorResponse> pageData = new PageDataResponse<>(page, limit, totalAuthors, responses);

            log.info("Retrieved {} authors (page {}/{})", responses.size(), page, totalPages);

            return new DatatableResponse<>(SUCCESS, "Authors retrieved successfully", HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting all authors", e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search) {
        try {
            int offset = (page - 1) * limit;
            List<Contributor> contributors = contributorMapper.findAllWithPagination(offset, limit, role, search);

            List<ContributorResponse> responses = contributors.stream().map(this::mapToContributorResponse).toList();

            int totalContributors = contributorMapper.countAll(role, search);
            int totalPages = (int) Math.ceil((double) totalContributors / limit);

            PageDataResponse<ContributorResponse> pageData = new PageDataResponse<>(page, limit, totalContributors, responses);

            log.info("Retrieved {} contributors (page {}/{})", responses.size(), page, totalPages);

            return new DatatableResponse<>(SUCCESS, "Contributors retrieved successfully", HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting all contributors", e);
            throw e;
        }
    }

    private GenreResponse mapToGenreResponse(Genre genre) {
        GenreResponse response = new GenreResponse();
        response.setId(genre.getId());
        response.setName(genre.getName());
        response.setSlug(genre.getSlug());
        response.setDescription(genre.getDescription());
        response.setBookCount(genre.getBookCount() != null ? genre.getBookCount() : 0);
        response.setCreatedAt(genre.getCreatedAt());
        return response;
    }

    private AuthorResponse mapToAuthorResponse(Author author) {
        AuthorResponse response = new AuthorResponse();
        response.setId(author.getId());
        response.setName(author.getName());
        response.setSlug(author.getSlug());
        response.setBirthDate(author.getBirthDate());
        response.setDeathDate(author.getDeathDate());
        response.setBirthPlace(author.getBirthPlace());
        response.setNationality(author.getNationality());
        response.setBiography(author.getBiography());
        response.setPhotoUrl(author.getPhotoUrl());
        response.setTotalBooks(author.getTotalBooks());
        response.setCreatedAt(author.getCreatedAt());
        response.setUpdatedAt(author.getUpdatedAt());
        return response;
    }

    private ContributorResponse mapToContributorResponse(Contributor contributor) {
        ContributorResponse response = new ContributorResponse();
        response.setId(contributor.getId());
        response.setName(contributor.getName());
        response.setSlug(contributor.getSlug());
        response.setRole(contributor.getRole());
        response.setWebsiteUrl(contributor.getWebsiteUrl());
        response.setTotalBooks(contributor.getTotalBooks() != null ? contributor.getTotalBooks() : 0);
        response.setCreatedAt(contributor.getCreatedAt());
        response.setUpdatedAt(contributor.getUpdatedAt());
        return response;
    }

    @Override
    public List<Book> getAllBooksForSitemap() {
        return bookMapper.findAllBooksForSitemap();
    }

    @Override
    public List<String> getChapterPaths(String bookSlug) {
        try {
            return bookMapper.getChapterPathsForSitemap(bookSlug);
        } catch (Exception e) {
            log.error("Error getting chapter paths for book slug: {}", bookSlug, e);
            return List.of();
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
            log.debug("No authenticated user found, treating as guest");
            return null;
        }
    }

    @Override
    public DatatableResponse<BookResponse> getBooksBySeries(String seriesSlug, int page, int limit) {
        try {
            int offset = (page - 1) * limit;

            List<BookResponse> books = bookMapper.getBooksBySeriesSlug(seriesSlug, offset, limit);
            int totalCount = bookMapper.countBooksBySeriesSlug(seriesSlug);

            log.info("Found {} books in series '{}'", totalCount, seriesSlug);

            PageDataResponse<BookResponse> data = new PageDataResponse<>(page, limit, totalCount, books);
            return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);

        } catch (Exception e) {
            log.error("Error fetching books by series slug: {}", seriesSlug, e);
            throw e;
        }
    }
}