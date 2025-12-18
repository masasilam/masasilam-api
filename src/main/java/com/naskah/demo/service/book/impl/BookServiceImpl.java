package com.naskah.demo.service.book.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.book.BookService;
import com.naskah.demo.service.book.EpubService;
import com.naskah.demo.util.file.EpubMetadataExtractor;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import com.naskah.demo.util.translation.MicrosoftTranslatorUtil;
import com.naskah.demo.util.tts.MicrosoftTTSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
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
    private final HeaderHolder headerHolder;
    private final ReactionMapper reactionMapper;

    private static final String SUCCESS = "Success";

    private final EpubService epubService;

    @Value("${file.upload.max-size:52428800}") // Default 50MB
    private String maxFileSizeStr;

    // ============ BOOK CRUD OPERATIONS ============
    public DataResponse<BookResponse> createBook(BookRequest request) {
        try {
            // Validate authentication
            if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty()) {
                throw new UnauthorizedException();
            }

            // Validate file
            long maxSizeBytes = FileUtil.parseFileSize(maxFileSizeStr);
            FileUtil.validateFile(request.getBookFile(), maxSizeBytes);

            // Validate EPUB format
            String fileExtension = FileUtil.getFileExtension(request.getBookFile().getOriginalFilename());
            if (!"epub".equalsIgnoreCase(fileExtension)) {
                throw new IllegalArgumentException("Only EPUB files are supported for auto-metadata extraction");
            }

            // =============== EXTRACT COMPLETE METADATA FROM EPUB ===============
            log.info("Extracting complete metadata from EPUB file");

            CompleteEpubMetadata epubMeta;
            try {
                epubMeta = EpubMetadataExtractor.extractCompleteMetadata(request.getBookFile().getInputStream());
            } catch (Exception e) {
                log.error("Failed to extract EPUB metadata: {}", e.getMessage());
                throw new RuntimeException("Invalid EPUB file or missing metadata: " + e.getMessage());
            }

            String finalTitle = epubMeta.getTitle();
            String finalSubtitle = epubMeta.getSubtitle();
            String finalDescription = epubMeta.getDescription();
            String finalPublisher = epubMeta.getPublisher();
            String finalCategory = epubMeta.getCategory();

            // =============== VALIDATION ===============
            if (finalTitle == null || finalTitle.isEmpty()) {
                throw new IllegalArgumentException("Title not found in EPUB metadata.");
            }

            if (finalPublisher == null || finalPublisher.isEmpty()) {
                throw new IllegalArgumentException("Publisher not found in EPUB metadata.");
            }

            if (epubMeta.getPublicationYear() == null) {
                throw new IllegalArgumentException("Publication year not found in EPUB metadata.");
            }

            // =============== GENERATE UNIQUE SLUG ===============
            String baseSlug = FileUtil.sanitizeFilename(finalTitle);
            String finalSlug = baseSlug;

            int duplicateCount = bookMapper.countBySlug(finalSlug);
            if (duplicateCount > 0) {
                finalSlug = baseSlug + "-" + System.currentTimeMillis();
                log.info("Slug '{}' already exists, using unique slug: {}", baseSlug, finalSlug);
            }

            // =============== GET OR CREATE LANGUAGE ===============
            Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
            if (language == null) {
                throw new IllegalArgumentException("Language '" + epubMeta.getLanguage() + "' not found in database.");
            }

            // =============== GET OR CREATE COPYRIGHT STATUS ===============
            CopyrightStatus copyrightStatus = copyrightStatusMapper
                    .findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

            if (copyrightStatus == null) {
                copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode("UNKNOWN");
                if (copyrightStatus == null) {
                    throw new RuntimeException("Default copyright status not found in database");
                }
            }

            // =============== UPLOAD COVER IMAGE ===============
            FileStorageResult coverResult;

            if (epubMeta.getCoverImageData() != null) {
                String coverUrl = FileUtil.uploadBookCoverFromBytes(
                        epubMeta.getCoverImageData(),
                        finalTitle,
                        null
                );
                coverResult = new FileStorageResult(coverUrl);
                log.info("Using cover image from EPUB");
            } else {
                throw new IllegalArgumentException("No cover image found in EPUB.");
            }

            // =============== UPLOAD BOOK FILE ===============
            FileStorageResult bookResult = FileUtil.saveAndUploadBookFile(request.getBookFile(), finalTitle);

            // Extract basic book metadata
            BookMetadata metadata = FileUtil.extractBookMetadata(request.getBookFile());

            // =============== CREATE BOOK ENTITY ===============
            Book book = new Book();
            book.setTitle(finalTitle);
            book.setSlug(finalSlug);
            book.setSubtitle(finalSubtitle);
            book.setSeriesId(null);
            book.setSeriesOrder(null);
            book.setEdition(1);
            book.setPublicationYear(epubMeta.getPublicationYear());
            book.setPublisher(finalPublisher);
            book.setLanguageId(language.getId());
            book.setDescription(finalDescription);
            book.setCoverImageUrl(coverResult.getCloudUrl());
            book.setFileUrl(bookResult.getCloudUrl());
            book.setSource(epubMeta.getSource());
            book.setFileFormat(metadata.getFileFormat());
            book.setFileSize(metadata.getFileSize());
            book.setTotalPages(metadata.getTotalPages());
            book.setTotalWord(metadata.getTotalWord());
            book.setEstimatedReadTime(FileUtil.calculateEstimatedReadTime(metadata.getTotalWord()));
            book.setCopyrightStatusId(copyrightStatus.getId());
            book.setViewCount(0);
            book.setReadCount(0);
            book.setDownloadCount(0);
            book.setIsActive(true);
            book.setIsFeatured(false);
            book.setPublishedAt(epubMeta.getPublishedAt() != null ? epubMeta.getPublishedAt().atStartOfDay() : null);
            book.setCategory(finalCategory);
            if (epubMeta.getUpdatedAt() != null) {
                book.setCreatedAt(epubMeta.getUpdatedAt());
                book.setUpdatedAt(epubMeta.getUpdatedAt());
                log.info("Using EPUB updated_at: {}", epubMeta.getUpdatedAt());
            } else {
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                log.info("Using current timestamp (no dcterms:modified in EPUB)");
            }
            bookMapper.insertBook(book);
            log.info("Book created with ID: {} and slug: {}", book.getId(), book.getSlug());

            // =============== PROCESS EPUB CHAPTERS (WITH HTML & IMAGES) ===============
            try {
                EpubProcessResult result = epubService.processEpubFile(request.getBookFile(), book);

                book.setTotalWord(result.getTotalWords());
                book.setTotalPages(result.getTotalChapters());
                book.setEstimatedReadTime(FileUtil.calculateEstimatedReadTime(result.getTotalWords()));

                // Update cover only if not null
                if (result.getCoverImageUrl() != null) {
                    book.setCoverImageUrl(result.getCoverImageUrl());
                }

                bookMapper.updateBook(book);

                log.info("EPUB processed: {} chapters, {} words",
                        result.getTotalChapters(), result.getTotalWords());

            } catch (Exception e) {
                log.error("Failed to process EPUB chapters: {}", e.getMessage());
                // Continue without chapters
            }

            // =============== AUTO-CREATE GENRES FROM SUBJECTS ===============
            if (epubMeta.getSubjects() != null && !epubMeta.getSubjects().isEmpty()) {
                for (String subject : epubMeta.getSubjects()) {
                    Genre genre = genreMapper.findByName(subject);

                    if (genre == null) {
                        genre = new Genre();
                        genre.setName(subject);
                        genre.setSlug(FileUtil.sanitizeFilename(subject));
                        genre.setDescription("Auto-generated from EPUB metadata");
                        genre.setCreatedAt(Instant.from(LocalDateTime.now()));
                        genreMapper.insertGenre(genre);
                        log.info("Auto-created genre: {}", subject);
                    }

                    bookMapper.insertBookGenre(book.getId(), genre.getId());
                }
            }

            // =============== AUTO-CREATE AUTHORS ===============
            if (epubMeta.getAuthors() != null && !epubMeta.getAuthors().isEmpty()) {
                for (AuthorMetadata authorMeta : epubMeta.getAuthors()) {
                    Author author = authorMapper.findAuthorByName(authorMeta.getName());

                    if (author != null) {
                        author.setTotalBooks(author.getTotalBooks() + 1);
                        author.setUpdatedAt(LocalDateTime.now());
                        authorMapper.updateAuthor(author);
                        bookMapper.insertBookAuthor(book.getId(), author.getId());
                    } else {
                        Author newAuthor = new Author();
                        newAuthor.setName(authorMeta.getName());
                        newAuthor.setSlug(FileUtil.sanitizeFilename(authorMeta.getName()));
                        newAuthor.setBirthDate(null);
                        newAuthor.setDeathDate(null);
                        newAuthor.setBirthPlace(null);
                        newAuthor.setNationality(null);
                        newAuthor.setBiography("Author information extracted from EPUB metadata");
                        newAuthor.setPhotoUrl(null);
                        newAuthor.setTotalBooks(1);
                        newAuthor.setCreatedAt(LocalDateTime.now());
                        newAuthor.setUpdatedAt(LocalDateTime.now());

                        authorMapper.insertAuthor(newAuthor);
                        bookMapper.insertBookAuthor(book.getId(), newAuthor.getId());

                        log.info("Auto-created author: {}", newAuthor.getName());
                    }
                }
            }

            // =============== AUTO-CREATE CONTRIBUTORS ===============
            if (epubMeta.getContributors() != null && !epubMeta.getContributors().isEmpty()) {
                for (ContributorMetadata contribMeta : epubMeta.getContributors()) {
                    log.info("🔍 Processing contributor from EPUB: {} with role: {}",
                            contribMeta.getName(), contribMeta.getRole());

                    Contributor contributor = contributorMapper.findByNameAndRole(
                            contribMeta.getName(),
                            contribMeta.getRole()
                    );

                    if (contributor == null) {
                        contributor = new Contributor();
                        contributor.setName(contribMeta.getName());
                        contributor.setRole(contribMeta.getRole());
                        contributor.setWebsiteUrl(null);
                        contributor.setCreatedAt(LocalDateTime.now());
                        contributor.setUpdatedAt(LocalDateTime.now());

                        String contribBaseSlug = FileUtil.sanitizeFilename(contribMeta.getName());
                        String contribFinalSlug = contribBaseSlug;

                        Contributor existingBySlug = contributorMapper.findBySlug(contribFinalSlug);
                        if (existingBySlug != null) {
                            contribFinalSlug = contribBaseSlug + "-" + contribMeta.getRole().toLowerCase();

                            existingBySlug = contributorMapper.findBySlug(contribFinalSlug);
                            if (existingBySlug != null) {
                                contribFinalSlug = contribBaseSlug + "-" + System.currentTimeMillis();
                            }
                        }

                        contributor.setSlug(contribFinalSlug);

                        contributorMapper.insertContributor(contributor);

                        log.info("✅ Auto-created contributor: {} ({}) with slug: {}",
                                contributor.getName(), contributor.getRole(), contributor.getSlug());
                    } else {
                        log.info("✅ Using existing contributor: {} ({})",
                                contributor.getName(), contributor.getRole());
                    }

                    // ✅ CRITICAL FIX: Always use the role from contribMeta (source of truth)
                    String roleToInsert = contribMeta.getRole();

                    bookMapper.insertBookContributor(
                            book.getId(),
                            contributor.getId(),
                            roleToInsert
                    );

                    log.info("✅ Inserted book_contributor: bookId={}, contributorId={}, role={}",
                            book.getId(), contributor.getId(), roleToInsert);
                }
            }

            // =============== GET COMPLETE BOOK RESPONSE ===============
            BookResponse data = bookMapper.getBookDetailBySlug(book.getSlug());

            log.info("✅ Book successfully created with full automation: {}", finalTitle);

            return new DataResponse<>(
                    SUCCESS,
                    ResponseMessage.DATA_CREATED,
                    201,
                    data
            );

        } catch (IOException e) {
            log.error("Error creating book: {}", e.getMessage(), e);
            throw new RuntimeException("Error when saving book", e);
        }
    }

    @Override
    public DataResponse<BookResponse> getBookDetailBySlug(String slug) {
        try {
            BookResponse data = bookMapper.getBookDetailBySlug(slug);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
            } else {
                throw new DataNotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when get book detail", e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<BookResponse> getPaginatedBooks(
            int page,
            int limit,
            String sortField,
            String sortOrder,
            BookSearchCriteria criteria) {
        try {
            // Validate and map sort fields
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

            // Get sort column, default to updated_at if not found
            String sortColumn = allowedSortFields.getOrDefault(sortField, "b.updated_at");

            // Validate sort order
            String sortType = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

            // Calculate offset
            int offset = (page - 1) * limit;

            // Log search criteria
            log.info("Fetching books with criteria: {}", criteria);
            log.info("Sort by: {} {}, Page: {}, Limit: {}", sortColumn, sortType, page, limit);

            // Get filtered results
            List<BookResponse> pageResult = bookMapper.getBookListWithAdvancedFilters(
                    criteria, offset, limit, sortColumn, sortType);

            // Get total count for pagination
            int totalCount = bookMapper.countBooksWithAdvancedFilters(criteria);

            log.info("Found {} books, returning page {} with {} items",
                    totalCount, page, pageResult.size());

            // Build paginated response
            PageDataResponse<BookResponse> data = new PageDataResponse<>(
                    page, limit, totalCount, pageResult);

            return new DatatableResponse<>(
                    SUCCESS,
                    ResponseMessage.DATA_FETCHED,
                    HttpStatus.OK.value(),
                    data);

        } catch (Exception e) {
            log.error("Error fetching paginated books with advanced filters", e);
            throw new RuntimeException("Failed to fetch books: " + e.getMessage(), e);
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

                // Use FileUtil.saveFile method properly
                Path savedFilePath = FileUtil.saveFile(file, "uploads", id);
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

    // ============ READING & DOWNLOAD OPERATIONS ============

    @Override
    @Transactional
    public ResponseEntity<byte[]> downloadBookAsBytes(String slug) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            String username = headerHolder.getUsername();
            boolean isAuthenticated = username != null && !username.isEmpty();

            // ✅ TAMBAHKAN: Update download count untuk SEMUA pengguna
            bookMapper.incrementDownloadCount(book.getId());
            log.info("Increased download count for book: {} (ID: {})", book.getTitle(), book.getId());

            // Always allow download, but track differently for authenticated users
            if (isAuthenticated) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    // Log download activity for authenticated users
                    Map<String, Object> metadata = Map.of(
                            "action", "download_book",
                            "book_title", book.getTitle(),
                            "book_slug", book.getSlug(),
                            "device_info", Map.of(
                                    "type", headerHolder.getDeviceType(),
                                    "browser", headerHolder.getBrowser(),
                                    "os", headerHolder.getOs(),
                                    "ip", headerHolder.getIpAddress()
                            )
                    );

                    UserActivity activity = new UserActivity();
                    activity.setUserId(user.getId());
                    activity.setActivityType("download");
                    activity.setEntityType("BOOK");
                    activity.setEntityId(book.getId());

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        activity.setMetadata(objectMapper.writeValueAsString(metadata));
                    } catch (JsonProcessingException e) {
                        log.error("Gagal mengkonversi metadata ke JSON", e);
                        activity.setMetadata("{}");
                    }

                    activity.setCreatedAt(LocalDateTime.now());
                    userMapper.insertUserActivity(activity);
                }
            }

            String fileUrl = book.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                throw new DataNotFoundException();
            }

            byte[] fileContent;

            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // Download from remote URL
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);

                    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new RuntimeException("Gagal mengunduh file dari URL: " + fileUrl);
                    }

                    fileContent = response.getBody();
                    log.info("Successfully downloaded {} bytes from: {}", fileContent.length, fileUrl);

                } catch (Exception e) {
                    log.error("Error downloading from URL: {}", fileUrl, e);
                    throw new RuntimeException("Gagal mengunduh file dari URL: " + e.getMessage());
                }
            } else {
                // Read local file
                try {
                    Path path = Paths.get(fileUrl);
                    fileContent = Files.readAllBytes(path);
                    log.info("Successfully read {} bytes from local file: {}", fileContent.length, fileUrl);

                } catch (Exception e) {
                    log.error("Error reading local file: {}", fileUrl, e);
                    throw new RuntimeException("Gagal membaca file lokal: " + e.getMessage());
                }
            }

            // Prepare filename
            String filename = book.getTitle() != null ?
                    sanitizeFilename(book.getTitle()) + ".epub" :
                    slug + ".epub";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (DataNotFoundException e) {
            log.warn("Book not found for download: {}", slug);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error downloading book: {}", slug, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================
    // GENRE OPERATIONS
    // ============================================

    @Override
    public DataResponse<List<GenreResponse>> getAllGenres(boolean includeBookCount) {
        try {
            List<Genre> genres;

            if (includeBookCount) {
                genres = genreMapper.findAllWithBookCount();
            } else {
                genres = genreMapper.findAll();
            }

            List<GenreResponse> responses = genres.stream()
                    .map(this::mapToGenreResponse)
                    .collect(Collectors.toList());

            log.info("Retrieved {} genres", responses.size());

            return new DataResponse<>(SUCCESS, "Genres retrieved successfully",
                    HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting all genres", e);
            throw e;
        }
    }

    // ============================================
    // AUTHOR OPERATIONS
    // ============================================

    @Override
    public DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy) {
        try {
            // Validate and map sort field
            Map<String, String> sortFieldMap = new HashMap<>();
            sortFieldMap.put("name", "name");
            sortFieldMap.put("bookCount", "total_books");
            sortFieldMap.put("createdAt", "created_at");

            String sortColumn = sortFieldMap.getOrDefault(sortBy, "name");

            int offset = (page - 1) * limit;

            List<Author> authors = authorMapper.findAllWithPagination(
                    offset, limit, search, sortColumn);

            List<AuthorResponse> responses = authors.stream()
                    .map(this::mapToAuthorResponse)
                    .collect(Collectors.toList());

            int totalAuthors = authorMapper.countAll(search);
            int totalPages = (int) Math.ceil((double) totalAuthors / limit);

            PageDataResponse<AuthorResponse> pageData = new PageDataResponse<>(
                    page, limit, totalAuthors, responses);

            log.info("Retrieved {} authors (page {}/{})", responses.size(), page, totalPages);

            return new DatatableResponse<>(SUCCESS, "Authors retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting all authors", e);
            throw e;
        }
    }

    // ============================================
    // CONTRIBUTOR OPERATIONS
    // ============================================

    @Override
    public DatatableResponse<ContributorResponse> getAllContributors(
            int page, int limit, String role, String search) {
        try {
            int offset = (page - 1) * limit;

            List<Contributor> contributors = contributorMapper.findAllWithPagination(
                    offset, limit, role, search);

            List<ContributorResponse> responses = contributors.stream()
                    .map(this::mapToContributorResponse)
                    .collect(Collectors.toList());

            int totalContributors = contributorMapper.countAll(role, search);
            int totalPages = (int) Math.ceil((double) totalContributors / limit);

            PageDataResponse<ContributorResponse> pageData = new PageDataResponse<>(
                    page, limit, totalContributors, responses);

            log.info("Retrieved {} contributors (page {}/{})", responses.size(), page, totalPages);

            return new DatatableResponse<>(SUCCESS, "Contributors retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting all contributors", e);
            throw e;
        }
    }

    // ============================================
    // MAPPER METHODS
    // ============================================

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

//    // ============ RATING OPERATIONS ============
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> addOrUpdateRating(String slug, RatingRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (request.getRating() == null || request.getRating() < 0.5 || request.getRating() > 5) {
//                throw new IllegalArgumentException("Rating must be between 0.5 and 5");
//            }
//
//            // Check if user already has rating for this book
//            Reaction existingRating = reactionMapper.findRatingByUserAndBook(user.getId(), book.getId());
//
//            Reaction savedRating;
//            String message;
//
//            if (existingRating != null) {
//                // Update existing rating
//                existingRating.setRating(request.getRating());
//                existingRating.setUpdatedAt(LocalDateTime.now());
//                reactionMapper.updateReaction(existingRating);
//                savedRating = existingRating;
//                message = "Rating updated successfully";
//            } else {
//                // Create new rating
//                Reaction rating = new Reaction();
//                rating.setUserId(user.getId());
//                rating.setBookId(book.getId());
//                rating.setReactionType("RATING");
//                rating.setRating(request.getRating());
//                rating.setComment(null);
//                rating.setTitle(null);
//                rating.setParentId(null);
//                rating.setCreatedAt(LocalDateTime.now());
//                rating.setUpdatedAt(LocalDateTime.now());
//
//                reactionMapper.insertReaction(rating);
//                savedRating = rating;
//                message = "Rating added successfully";
//            }
//
//            ReactionResponse response = mapToReactionResponse(savedRating, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error processing rating for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<Void> deleteRating(String slug) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Find user's rating for this book
//            Reaction rating = reactionMapper.findRatingByUserAndBook(user.getId(), book.getId());
//
//            if (rating == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Delete the rating
//            reactionMapper.deleteReaction(rating.getId());
//
//            return new DataResponse<>(SUCCESS, "Rating deleted successfully", HttpStatus.OK.value(), null);
//
//        } catch (Exception e) {
//            log.error("Error deleting rating for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//// ============ REVIEW/COMMENT OPERATIONS ============
//
//    @Override
//    public DataResponse<List<ReactionResponse>> getReviews(String slug, int page, int limit) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get current user for reaction status
//            String currentUsername = null;
//            Long currentUserId = null;
//            try {
//                currentUsername = headerHolder.getUsername();
//                if (currentUsername != null) {
//                    User currentUser = userMapper.findUserByUsername(currentUsername);
//                    if (currentUser != null) {
//                        currentUserId = currentUser.getId();
//                    }
//                }
//            } catch (Exception e) {
//                // User might not be logged in, continue without user info
//            }
//
//            // Get reviews with pagination
//            int offset = (page - 1) * limit;
//            List<Reaction> reviews = reactionMapper.findReviewsByBookIdWithPagination(book.getId(), offset, limit);
//
//            final Long finalCurrentUserId = currentUserId;
//            List<ReactionResponse> responses = reviews.stream()
//                    .map(review -> {
//                        User reviewUser = userMapper.findUserById(review.getUserId());
//                        return mapToReactionResponse(review, reviewUser, book.getId(), finalCurrentUserId);
//                    })
//                    .collect(Collectors.toList());
//
//            return new DataResponse<>(SUCCESS, "Reviews retrieved successfully", HttpStatus.OK.value(), responses);
//
//        } catch (Exception e) {
//            log.error("Error getting reviews for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> addReview(String slug, ReviewRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
//                throw new IllegalArgumentException("Review content is required");
//            }
//
//            // Check if user already has review for this book
//            Reaction existingReview = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());
//
//            if (existingReview != null) {
//                throw new IllegalArgumentException("You already have a review for this book. Use update endpoint to modify it.");
//            }
//
//            // Create new review
//            Reaction review = new Reaction();
//            review.setUserId(user.getId());
//            review.setBookId(book.getId());
//            review.setReactionType("COMMENT");
//            review.setComment(request.getComment());
//            review.setTitle(request.getTitle());
//            review.setRating(null);
//            review.setParentId(null);
//            review.setCreatedAt(LocalDateTime.now());
//            review.setUpdatedAt(LocalDateTime.now());
//
//            reactionMapper.insertReaction(review);
//
//            ReactionResponse response = mapToReactionResponse(review, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, "Review added successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding review for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> updateReview(String slug, ReviewRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
//                throw new IllegalArgumentException("Review content is required");
//            }
//
//            // Find user's review for this book
//            Reaction existingReview = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());
//
//            if (existingReview == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Update review
//            existingReview.setComment(request.getComment());
//            existingReview.setTitle(request.getTitle());
//            existingReview.setUpdatedAt(LocalDateTime.now());
//            reactionMapper.updateReaction(existingReview);
//
//            ReactionResponse response = mapToReactionResponse(existingReview, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, "Review updated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error updating review for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<Void> deleteReview(String slug) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Find user's review for this book
//            Reaction review = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());
//
//            if (review == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Delete the review and all its replies/feedback
//            reactionMapper.deleteReactionAndReplies(review.getId());
//
//            return new DataResponse<>(SUCCESS, "Review deleted successfully", HttpStatus.OK.value(), null);
//
//        } catch (Exception e) {
//            log.error("Error deleting review for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//// ============ REPLY OPERATIONS ============
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> addReply(String slug, Long parentId, ReplyRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
//                throw new IllegalArgumentException("Reply content is required");
//            }
//
//            // Verify parent review/comment exists
//            Reaction parentReaction = reactionMapper.findReactionById(parentId);
//            if (parentReaction == null) {
//                throw new DataNotFoundException();
//            }
//
//            // User cannot reply to their own review/comment
//            if (parentReaction.getUserId().equals(user.getId())) {
//                throw new IllegalArgumentException("Cannot reply to your own review/comment");
//            }
//
//            // Verify parent belongs to the same book
//            if (!parentReaction.getBookId().equals(book.getId())) {
//                throw new IllegalArgumentException("Parent reaction does not belong to this book");
//            }
//
//            // Create reply
//            Reaction reply = new Reaction();
//            reply.setUserId(user.getId());
//            reply.setBookId(book.getId());
//            reply.setReactionType("COMMENT");
//            reply.setComment(request.getComment());
//            reply.setTitle(null);
//            reply.setParentId(parentId);
//            reply.setRating(null);
//            reply.setCreatedAt(LocalDateTime.now());
//            reply.setUpdatedAt(LocalDateTime.now());
//
//            reactionMapper.insertReaction(reply);
//
//            ReactionResponse response = mapToReactionResponse(reply, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, "Reply added successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding reply for review: {}", parentId, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> updateReply(String slug, Long replyId, ReplyRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
//                throw new IllegalArgumentException("Reply content is required");
//            }
//
//            // Get the reply to verify ownership
//            Reaction reply = reactionMapper.findReactionById(replyId);
//            if (reply == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Verify it's a reply (has parentId)
//            if (reply.getParentId() == null) {
//                throw new IllegalArgumentException("This is not a reply");
//            }
//
//            // Check if user owns the reply
//            if (!reply.getUserId().equals(user.getId())) {
//                throw new UnauthorizedException();
//            }
//
//            // Update the reply
//            reply.setComment(request.getComment());
//            reply.setUpdatedAt(LocalDateTime.now());
//            reactionMapper.updateReaction(reply);
//
//            ReactionResponse response = mapToReactionResponse(reply, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, "Reply updated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error updating reply: {}", replyId, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<Void> deleteReply(String slug, Long replyId) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get the reply to verify ownership
//            Reaction reply = reactionMapper.findReactionById(replyId);
//            if (reply == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Verify it's a reply (has parentId)
//            if (reply.getParentId() == null) {
//                throw new IllegalArgumentException("This is not a reply");
//            }
//
//            // Check if user owns the reply
//            if (!reply.getUserId().equals(user.getId())) {
//                throw new UnauthorizedException();
//            }
//
//            // Delete the reply and its nested replies
//            reactionMapper.deleteReactionAndReplies(replyId);
//
//            return new DataResponse<>(SUCCESS, "Reply deleted successfully", HttpStatus.OK.value(), null);
//
//        } catch (Exception e) {
//            log.error("Error deleting reply: {}", replyId, e);
//            throw e;
//        }
//    }
//
//// ============ FEEDBACK OPERATIONS (HELPFUL/NOT_HELPFUL) ============
//
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> addOrUpdateFeedback(String slug, Long reviewId, FeedbackRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Validate feedback type
//            String feedbackType = request.getType().toUpperCase();
//            if (!feedbackType.equals("HELPFUL") && !feedbackType.equals("NOT_HELPFUL")) {
//                throw new IllegalArgumentException("Feedback type must be HELPFUL or NOT_HELPFUL");
//            }
//
//            // Verify review exists
//            Reaction review = reactionMapper.findReactionById(reviewId);
//            if (review == null) {
//                throw new DataNotFoundException();
//            }
//
//            // User cannot give feedback to their own review
//            if (review.getUserId().equals(user.getId())) {
//                throw new IllegalArgumentException("Cannot give feedback to your own review");
//            }
//
//            // Check if user already gave feedback on this review
//            Reaction existingFeedback = reactionMapper.findFeedbackByUserAndReview(user.getId(), reviewId);
//
//            Reaction savedFeedback;
//            String message;
//
//            if (existingFeedback != null) {
//                // Update existing feedback
//                existingFeedback.setReactionType(feedbackType);
//                existingFeedback.setUpdatedAt(LocalDateTime.now());
//                reactionMapper.updateReaction(existingFeedback);
//                savedFeedback = existingFeedback;
//                message = "Feedback updated successfully";
//            } else {
//                // Create new feedback
//                Reaction feedback = new Reaction();
//                feedback.setUserId(user.getId());
//                feedback.setBookId(book.getId());
//                feedback.setReactionType(feedbackType);
//                feedback.setComment(null);
//                feedback.setTitle(null);
//                feedback.setRating(null);
//                feedback.setParentId(reviewId);
//                feedback.setCreatedAt(LocalDateTime.now());
//                feedback.setUpdatedAt(LocalDateTime.now());
//
//                reactionMapper.insertReaction(feedback);
//                savedFeedback = feedback;
//                message = "Feedback added successfully";
//            }
//
//            ReactionResponse response = mapToReactionResponse(savedFeedback, user, book.getId(), user.getId());
//            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error processing feedback for review: {}", reviewId, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<Void> deleteFeedback(String slug, Long reviewId) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (user == null || book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Find user's feedback for this review
//            Reaction feedback = reactionMapper.findFeedbackByUserAndReview(user.getId(), reviewId);
//
//            if (feedback == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Delete the feedback
//            reactionMapper.deleteReaction(feedback.getId());
//
//            return new DataResponse<>(SUCCESS, "Feedback deleted successfully", HttpStatus.OK.value(), null);
//
//        } catch (Exception e) {
//            log.error("Error deleting feedback for review: {}", reviewId, e);
//            throw e;
//        }
//    }

    // ============ SEARCH & TRANSLATION OPERATIONS ============
    @Override
    public DataResponse<SearchResultResponse> searchInBook(String slug, String query, int page, int limit) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Read book content and perform search
            String bookContent = readBookContent(book.getFileUrl());
            List<SearchResultResponse.SearchResult> results = performTextSearch(bookContent, query, page, limit);

            SearchResultResponse response = new SearchResultResponse();
            response.setQuery(query);
            response.setTotalResults(results.size());
            response.setCurrentPage(page);
            response.setTotalPages((int) Math.ceil((double) results.size() / limit));
            response.setResults(results);

            return new DataResponse<>(SUCCESS, "Search completed successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error searching in book: {} query: {}", slug, query, e);
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }


    private ReactionResponse mapToReactionResponse(Reaction reaction, User reactionUser, Long bookId, Long currentUserId) {
        ReactionResponse response = new ReactionResponse();
        response.setId(reaction.getId());
        response.setUserId(reaction.getUserId());
        response.setUserName(reactionUser != null ? reactionUser.getUsername() : "Unknown");
        response.setBookId(reaction.getBookId());
        response.setReactionType(reaction.getReactionType());
        response.setRating(reaction.getRating());
        response.setComment(reaction.getComment());
        response.setTitle(reaction.getTitle());
        response.setParentId(reaction.getParentId());
        response.setCreatedAt(reaction.getCreatedAt());
        response.setUpdatedAt(reaction.getUpdatedAt());

        // Get reply count if this is a parent reaction
        if (reaction.getParentId() == null) {
            try {
                int replyCount = reactionMapper.countRepliesByParentId(reaction.getId());
                response.setReplyCount(replyCount);
            } catch (Exception e) {
                response.setReplyCount(0);
            }
        } else {
            response.setReplyCount(0);
        }

        // Add stats to response - get once per reaction to avoid performance issues
        try {
            ReactionStatsResponse stats = reactionMapper.getReactionStats(bookId);
            if (stats != null) {
            } else {
                setDefaultStats(response);
            }

            if (currentUserId != null) {
                String userReactionType = reactionMapper.getUserReactionType(currentUserId, bookId);
            } else {
            }
        } catch (Exception e) {
            log.warn("Failed to get reaction stats: {}", e.getMessage());
            setDefaultStats(response);
        }

        return response;
    }

    private void setDefaultStats(ReactionResponse response) {
    }

    private String readBookContent(String fileUrl) throws IOException {
        try {
            // If it's a local file path
            if (fileUrl.startsWith("file://") || !fileUrl.startsWith("http")) {
                String filePath = fileUrl.replace("file://", "");
                Path path = Paths.get(filePath);
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            // If it's a URL, download and read
            else {
                URL url = new URL(fileUrl);
                try (InputStream inputStream = url.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (Exception e) {
            log.error("Error reading book content from: {}", fileUrl, e);
            throw new IOException("Failed to read book content", e);
        }
    }

    private List<SearchResultResponse.SearchResult> performTextSearch(String content, String query, int page, int limit) {
        List<SearchResultResponse.SearchResult> results = new ArrayList<>();

        if (content == null || query == null || query.trim().isEmpty()) {
            return results;
        }

        String[] lines = content.split("\n");
        String lowerQuery = query.toLowerCase();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lowerLine = line.toLowerCase();

            if (lowerLine.contains(lowerQuery)) {
                SearchResultResponse.SearchResult result = new SearchResultResponse.SearchResult();
                result.setPage((i / 30) + 1); // Assuming 30 lines per page
                result.setContext(getSearchContext(lines, i)); // 2 lines context
                result.setHighlightedText(highlightSearchTerm(line, query));
                result.setPosition(String.valueOf(i + 1)); // Line number as position
                result.setRelevanceScore(calculateRelevanceScore(line, query)); // Calculate relevance
                results.add(result);
            }
        }

        // Apply pagination
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, results.size());

        if (startIndex < results.size()) {
            return results.subList(startIndex, endIndex);
        }

        return new ArrayList<>();
    }

    private String getSearchContext(String[] lines, int currentLine) {
        int start = Math.max(0, currentLine - 2);
        int end = Math.min(lines.length - 1, currentLine + 2);

        StringBuilder context = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (i == currentLine) {
                context.append(">>> ").append(lines[i]).append(" <<<");
            } else {
                context.append(lines[i]);
            }
            if (i < end) {
                context.append("\n");
            }
        }
        return context.toString();
    }

    private Double calculateRelevanceScore(String text, String searchTerm) {
        if (text == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return 0.0;
        }

        String lowerText = text.toLowerCase();
        String lowerTerm = searchTerm.toLowerCase().trim();

        // Base score for containing the term
        double score = 0.5;

        // Bonus for exact match
        if (lowerText.contains(lowerTerm)) {
            score += 0.3;
        }

        // Bonus for word boundary match
        if (lowerText.matches(".*\\b" + Pattern.quote(lowerTerm) + "\\b.*")) {
            score += 0.2;
        }

        // Penalty for longer text (shorter matches are more relevant)
        if (text.length() > 100) {
            score -= 0.1;
        }

        // Bonus for multiple occurrences
        long occurrences = lowerText.split(Pattern.quote(lowerTerm), -1).length - 1;
        if (occurrences > 1) {
            score += Math.min(0.2, occurrences * 0.05);
        }

        return Math.max(0.0, Math.min(1.0, score)); // Clamp between 0.0 and 1.0
    }

    private String highlightSearchTerm(String text, String searchTerm) {
        if (text == null || searchTerm == null) {
            return text;
        }
        return text.replaceAll("(?i)" + Pattern.quote(searchTerm),
                "<mark>$0</mark>");
    }

    private String sanitizeFilename(String filename) {
        return filename.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}