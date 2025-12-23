package com.naskah.demo.service.book.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.InternalServerErrorException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

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
    private final EpubService epubService;
    private final FileUtil fileUtil;
    private static final String SUCCESS = "Success";

    @Value("${file.upload.max-size:52428800}")
    private String maxFileSizeStr;

    // ============ BOOK CRUD OPERATIONS ============
    public DataResponse<BookResponse> createBook(BookRequest request){
        try {
            if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty()) {
                throw new UnauthorizedException();
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
            String baseSlug = fileUtil.sanitizeFilename(finalTitle);
            String finalSlug = baseSlug;

            int duplicateCount = bookMapper.countBySlug(finalSlug);
            if (duplicateCount > 0) {
                finalSlug = baseSlug + "-" + System.currentTimeMillis();
                log.info("Slug '{}' already exists, using unique slug: {}", baseSlug, finalSlug);
            }

            // =============== GET OR CREATE LANGUAGE ===============
            Language language = languageMapper.findLanguageByName(epubMeta.getLanguage());
            CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(epubMeta.getCopyrightStatus());

            // =============== UPLOAD BOOK FILE ===============
            FileStorageResult bookResult = fileUtil.saveAndUploadBookFile(request.getBookFile(), finalTitle);
            BookMetadata metadata = fileUtil.extractBookMetadata(request.getBookFile());

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

            bookMapper.insertBook(book);
            log.info("Book created with ID: {} and slug: {}", book.getId(), book.getSlug());

            EpubProcessResult result = epubService.processEpubFile(request.getBookFile(), book);
            log.info("EPUB processed: {} chapters, {} words", result.getTotalChapters(), result.getTotalWords());

            book.setTotalWord(result.getTotalWords());
            book.setTotalPages(result.getTotalChapters());
            book.setEstimatedReadTime(fileUtil.calculateEstimatedReadTime(result.getTotalWords()));
            book.setCoverImageUrl(result.getCoverImageUrl());

            bookMapper.updateBook(book);

            genreProcessing(epubMeta, book);
            authorProcessing(epubMeta, book);
            contributorProcessing(epubMeta, book);

            // =============== GET COMPLETE BOOK RESPONSE ===============
            BookResponse data = bookMapper.getBookDetailBySlug(book.getSlug());

            log.info("Book successfully created with full automation: {}", finalTitle);

            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, 201, data);

        } catch (Exception e) {
            log.error("Error creating book: {}", e.getMessage(), e);
            throw new InternalServerErrorException();
        }
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
                    genre.setCreatedAt(Instant.from(LocalDateTime.now()));
                    genreMapper.insertGenre(genre);
                    log.info("Auto-created genre: {}", subject);
                }

                bookMapper.insertBookGenre(book.getId(), genre.getId());
            }
        }
    }

    private void authorProcessing(CompleteEpubMetadata epubMeta, Book book) {
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
                    newAuthor.setSlug(fileUtil.sanitizeFilename(authorMeta.getName()));
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

                log.info("Contributor created with ID: {}", contributor.getId());
                log.info("Auto-created contributor: {} ({}) with slug: {}", contributor.getName(), contributor.getRole(), contributor.getSlug());
            } else {
                log.info("Using existing contributor from DB: {} (ID: {})", contributor.getName(), contributor.getId());
            }

            String roleToInsert = contribMeta.getRole();

            bookMapper.insertBookContributor(book.getId(), contributor.getId(), roleToInsert);

            log.info("Inserted book_contributor: bookId={}, contributorId={}, role={}",
                    book.getId(), contributor.getId(), roleToInsert);
        }

        log.info("=== CONTRIBUTOR PROCESSING END ===");
    }

    @Override
    @Transactional
    public DataResponse<BookResponse> getBookDetailBySlug(String slug) {
        try {
            bookMapper.incrementViewCountBySlug(slug);

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

            // Log search criteria
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

            bookMapper.incrementDownloadCount(book.getId());
            log.info("Increased download count for book: {} (ID: {})", book.getTitle(), book.getId());

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
                    activity.setMetadata(new ObjectMapper().writeValueAsString(metadata));

                    activity.setCreatedAt(LocalDateTime.now());
                    userMapper.insertUserActivity(activity);
                }
            }

            String fileUrl = book.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                throw new DataNotFoundException();
            }

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IOException("Failed to download file from URL: " + fileUrl);
            }

            byte[] fileContent = response.getBody();
            log.info("Successfully downloaded {} bytes from: {}", fileContent.length, fileUrl);

            String filename = book.getTitle() != null ? fileUtil.sanitizeFilename(book.getTitle()) + ".epub" : slug + ".epub";

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
                    .toList();

            log.info("Retrieved {} genres", responses.size());

            return new DataResponse<>(SUCCESS, "Genres retrieved successfully", HttpStatus.OK.value(), responses);

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
            Map<String, String> sortFieldMap = new HashMap<>();
            sortFieldMap.put("name", "name");
            sortFieldMap.put("bookCount", "total_books");
            sortFieldMap.put("createdAt", "created_at");

            String sortColumn = sortFieldMap.getOrDefault(sortBy, "name");

            int offset = (page - 1) * limit;
            List<Author> authors = authorMapper.findAllWithPagination(offset, limit, search, sortColumn);

            List<AuthorResponse> responses = authors.stream()
                    .map(this::mapToAuthorResponse)
                    .toList();

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

    // ============================================
    // CONTRIBUTOR OPERATIONS
    // ============================================

    @Override
    public DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search) {
        try {
            int offset = (page - 1) * limit;
            List<Contributor> contributors = contributorMapper.findAllWithPagination(offset, limit, role, search);

            List<ContributorResponse> responses = contributors.stream()
                    .map(this::mapToContributorResponse)
                    .toList();

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
}