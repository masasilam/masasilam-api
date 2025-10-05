package com.naskah.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.exception.custom.DataAlreadyExistsException;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.BookService;
import com.naskah.demo.util.file.BookMetadata;
import com.naskah.demo.util.file.FileStorageResult;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import com.naskah.demo.util.translation.MicrosoftTranslatorUtil;
import com.naskah.demo.util.tts.MicrosoftTTSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final ReadingMapper readingMapper;
    private final HeaderHolder headerHolder;

    // New mappers for additional features
    private final BookmarkMapper bookmarkMapper;
    private final HighlightMapper highlightMapper;
    private final NoteMapper noteMapper;
    private final ReactionMapper reactionMapper;
    private final DiscussionMapper discussionMapper;
    private final HighlightTranslationMapper highlightTranslationMapper;
    private final AudioSyncMapper audioSyncMapper;

    // Utility services
    private final MicrosoftTranslatorUtil translatorUtil;
    private final MicrosoftTTSUtil ttsUtil;

    private static final String SUCCESS = "Success";

    // ============ BOOK CRUD OPERATIONS ============
    @Override
    @Transactional
    public DataResponse<BookResponse> createBook(BookRequest request) {
        try {
            if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty()) {
                throw new UnauthorizedException();
            }

            int duplicateBook = bookMapper.countByTitleAndPublicationYear(request.getTitle(), request.getPublicationYear());
            if (duplicateBook > 0) {
                throw new DataAlreadyExistsException();
            }

            Language language = languageMapper.findLanguageByName(request.getLanguage());
            CopyrightStatus copyrightStatus = copyrightStatusMapper.findByCopyrightStatusCode(request.getCopyrightStatus());

            // Extract metadata dan upload files
            BookMetadata metadata = FileUtil.extractBookMetadata(request.getBookFile(), language.getId());
            FileStorageResult coverResult = FileUtil.saveAndUploadBookCover(request.getCoverImage(), request.getTitle());
            FileStorageResult bookResult = FileUtil.saveAndUploadBookFile(request.getBookFile(), request.getTitle());

            // Set book data
            Book book = new Book();
            book.setTitle(request.getTitle());
            book.setSlug(FileUtil.sanitizeFilename(request.getTitle()));
            book.setSubtitle(request.getSubtitle());
            book.setSeriesId(request.getSeriesId());
            book.setSeriesOrder(request.getSeriesOrder());
            book.setEdition(1);
            book.setPublicationYear(request.getPublicationYear());
            book.setPublisher(request.getPublisher());
            book.setLanguageId(language.getId());
            book.setDescription(request.getDescription());
            book.setSummary(request.getSummary());
            book.setCoverImageUrl(coverResult.getCloudUrl());
            book.setFileUrl(bookResult.getCloudUrl());
//            book.setFilePath(bookResult.getLocalPath());
//            book.setCoverImagePath(coverResult.getLocalPath());
            book.setFileFormat(metadata.getFileFormat());
            book.setFileSize(metadata.getFileSize());
            book.setTotalPages(metadata.getTotalPages());
            book.setTotalWord(metadata.getTotalWord());
            book.setEstimatedReadTime(FileUtil.calculateEstimatedReadTime(metadata.getTotalWord()));
            book.setDifficultyLevel(metadata.getDifficultyLevel());
            book.setCopyrightStatusId(copyrightStatus.getId());
            book.setViewCount(0);
            book.setReadCount(0);
            book.setDownloadCount(0);
            book.setIsActive(true);
            book.setIsFeatured(false);
            book.setPublishedAt(request.getPublishedAt());
            book.setCategory(request.getCategory());
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());

            bookMapper.insertBook(book);

            // Insert genres
            for (Long genreId : request.getGenreIds()) {
                bookMapper.insertBookGenre(book.getId(), genreId);
            }

            //insert authors
            for (AuthorRequest authorRequest : request.getAuthors()) {
                Author author = authorMapper.findAuthorByName(authorRequest.getName());

                if (author != null) {
                    int currentBookCount = author.getTotalBooks();
                    author.setTotalBooks(currentBookCount + 1);
                    author.setUpdatedAt(LocalDateTime.now());
                    authorMapper.updateAuthor(author);

                    bookMapper.insertBookAuthor(book.getId(), author.getId());
                } else {
                    Author newAuthor = new Author();
                    newAuthor.setName(authorRequest.getName());
                    newAuthor.setSlug(FileUtil.sanitizeFilename(authorRequest.getName()));
                    newAuthor.setBirthDate(authorRequest.getBirthDate());
                    newAuthor.setDeathDate(authorRequest.getDeathDate());
                    newAuthor.setBirthPlace(authorRequest.getBirthPlace());
                    newAuthor.setNationality(authorRequest.getNationality());
                    newAuthor.setBiography(authorRequest.getBiography());

                    if (authorRequest.getPhoto() != null) {
                        FileStorageResult authorPhotoResult = FileUtil.saveAndUploadAuthorPhoto(authorRequest.getPhoto(), authorRequest.getName());
                        newAuthor.setPhotoUrl(authorPhotoResult.getCloudUrl());
//                        newAuthor.setPhotoPath(authorPhotoResult.getLocalPath());
                    }

                    newAuthor.setTotalBooks(1);
                    newAuthor.setCreatedAt(LocalDateTime.now());
                    newAuthor.setUpdatedAt(LocalDateTime.now());

                    authorMapper.insertAuthor(newAuthor);

                    bookMapper.insertBookAuthor(book.getId(), newAuthor.getId());
                }
            }

            // Process contributors
            if (request.getContributors() != null && !request.getContributors().isEmpty()) {
                for (ContributorRequest contributorRequest : request.getContributors()) {
                    Contributor contributor = contributorMapper.findByNameAndRole(
                            contributorRequest.getName(), contributorRequest.getRole());

                    if (contributor == null) {
                        // Create new contributor
                        contributor = new Contributor();
                        contributor.setName(contributorRequest.getName());
                        contributor.setSlug(FileUtil.sanitizeFilename(contributorRequest.getName()));
                        contributor.setRole(contributorRequest.getRole());
                        contributor.setWebsiteUrl(contributorRequest.getWebsiteUrl());
                        contributor.setCreatedAt(LocalDateTime.now());
                        contributor.setUpdatedAt(LocalDateTime.now());
                        contributorMapper.insertContributor(contributor);
                    }

                    bookMapper.insertBookContributor(book.getId(), contributor.getId(), contributor.getRole());
                }
            }

            // Get complete book data with all relations
            BookResponse data = bookMapper.getBookDetailBySlug(book.getSlug());
            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, HttpStatus.CREATED.value(), data);

        } catch (IOException e) {
            throw new RuntimeException("Error when save a book", e);
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
    public DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder,
                                                             String searchTitle, Long seriesId, Long genreId, Long subGenreId) {
        try {
            Map<String, String> allowedSortFields = new HashMap<>();
            allowedSortFields.put("updateAt", "UPDATE_AT");
            allowedSortFields.put("title", "TITLE");
            allowedSortFields.put("publishedAt", "PUBLISHED_AT");
            allowedSortFields.put("author", "AUTHOR_NAME");
            allowedSortFields.put("estimatedReadTime", "ESTIMATED_READ_TIME");
            allowedSortFields.put("totalWord", "TOTAL_WORD");
            allowedSortFields.put("averageRating", "AVERAGE_RATING");
            allowedSortFields.put("viewCount", "VIEW_COUNT");
            allowedSortFields.put("readCount", "READ_COUNT");

            String sortColumn = allowedSortFields.getOrDefault(sortField, "UPDATE_AT");
            String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
            int offset = (page - 1) * limit;

            List<BookResponse> pageResult = bookMapper.getBookListWithFilters(
                    searchTitle, seriesId, genreId, subGenreId, offset, limit, sortColumn, sortType);

            PageDataResponse<BookResponse> data = new PageDataResponse<>(page, limit, pageResult.size(), pageResult);
            return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);

        } catch (Exception e) {
            log.error("Error fetching paginated books with filters", e);
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

                // Use FileUtil.saveFile method properly
                Path savedFilePath = FileUtil.saveFile(file, "uploads", id);
                book.setFilePath(savedFilePath.toString());
            } else {
                book.setFilePath(existingEbook.getFilePath());
            }

            bookMapper.updateEbook(book);
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
    public DataResponse<ReadingResponse> startReading(String slug) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            String username = headerHolder.getUsername();
            boolean isAuthenticated = username != null && !username.isEmpty();

            ReadingProgress existingProgress = null;
            ReadingSession session = null;
            Long userId = null;

            if (isAuthenticated) {
                // User is logged in - full functionality
                User user = userMapper.findUserByUsername(username);
                if (user == null) {
                    throw new DataNotFoundException();
                }
                userId = user.getId();

                // Handle reading progress for authenticated user
                existingProgress = readingMapper.findReadingProgressByUserAndBook(userId, book.getId());

                if (existingProgress == null) {
                    ReadingProgress newProgress = new ReadingProgress();
                    newProgress.setUserId(userId);
                    newProgress.setBookId(book.getId());
                    newProgress.setCurrentPage(1);
                    newProgress.setTotalPages(book.getTotalPages());
                    newProgress.setCurrentPosition("0");
                    newProgress.setPercentageCompleted(BigDecimal.ZERO);
                    newProgress.setReadingTimeMinutes(0);
                    newProgress.setStatus("READING");
                    newProgress.setIsFavorite(false);
                    newProgress.setStartedAt(LocalDateTime.now());
                    newProgress.setLastReadAt(LocalDateTime.now());

                    readingMapper.insertReadingProgress(newProgress);
                    existingProgress = newProgress;
                } else {
                    // Update last read time
                    existingProgress.setLastReadAt(LocalDateTime.now());
                    if (!"COMPLETED".equals(existingProgress.getStatus())) {
                        existingProgress.setStatus("READING");
                    }
                    readingMapper.updateReadingProgress(existingProgress);
                }

                // Create new reading session with device info
                session = new ReadingSession();
                session.setUserId(userId);
                session.setBookId(book.getId());
                session.setStartTime(LocalDateTime.now());
                session.setDeviceClass(headerHolder.getDeviceType());
                session.setDeviceName(headerHolder.getDeviceName());
                session.setDeviceBrand(headerHolder.getDeviceBrand());
                session.setAgentNameVersion(headerHolder.getBrowser());
                session.setOperatingSystem(headerHolder.getOs());
                session.setLayoutEngine(headerHolder.getLayoutEngine());
                session.setDeviceCpu(headerHolder.getDeviceCpu());
                session.setIpAddress(headerHolder.getIpAddress());
                session.setCreatedAt(LocalDateTime.now());

                readingMapper.insertReadingSession(session);

                // Create user activity for authenticated users
                Map<String, Object> metadata = Map.of(
                        "action", "start_reading",
                        "book_title", book.getTitle(),
                        "book_slug", book.getSlug(),
                        "device_info", Map.of(
                                "type", headerHolder.getDeviceType(),
                                "name", headerHolder.getDeviceName(),
                                "browser", headerHolder.getBrowser(),
                                "os", headerHolder.getOs(),
                                "ip", headerHolder.getIpAddress()
                        )
                );

                UserActivity activity = new UserActivity();
                activity.setUserId(userId);
                activity.setActivityType("read");
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

            // Always increment book view count (for both authenticated and guest users)
            bookMapper.incrementReadCount(book.getId());

            // Create reading response
            ReadingResponse readingResponse;
            if (isAuthenticated) {
                // Full response for authenticated users
                readingResponse = getReadingResponse(book, existingProgress, session);
            } else {
                // Limited response for guest users
                readingResponse = getGuestReadingResponse(book);
            }

            String message = isAuthenticated ?
                    "Berhasil memulai membaca buku" :
                    "Membaca sebagai tamu - Login untuk menyimpan progres";

            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), readingResponse);

        } catch (Exception e) {
            log.error("Error saat memulai membaca buku: {} untuk user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

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

    // ============ READING PROGRESS OPERATIONS ============
    @Override
    @Transactional
    public DataResponse<ReadingProgressResponse> saveReadingProgress(String slug, ProgressRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            ReadingProgress existingProgress = readingMapper.findReadingProgressByUserAndBook(user.getId(), book.getId());

            if (existingProgress != null) {
                // Update existing progress
                existingProgress.setCurrentPage(request.getCurrentPage());
                existingProgress.setCurrentPosition(request.getCurrentPosition());
                existingProgress.setPercentageCompleted(request.getPercentageCompleted());
                existingProgress.setReadingTimeMinutes(request.getReadingTimeMinutes());
                existingProgress.setStatus(request.getStatus());
                existingProgress.setLastReadAt(LocalDateTime.now());

                readingMapper.updateReadingProgress(existingProgress);
            } else {
                // Create new progress
                ReadingProgress newProgress = new ReadingProgress();
                newProgress.setUserId(user.getId());
                newProgress.setBookId(book.getId());
                newProgress.setCurrentPage(request.getCurrentPage());
                newProgress.setTotalPages(book.getTotalPages());
                newProgress.setCurrentPosition(request.getCurrentPosition());
                newProgress.setPercentageCompleted(request.getPercentageCompleted());
                newProgress.setReadingTimeMinutes(request.getReadingTimeMinutes());
                newProgress.setStatus(request.getStatus());
                newProgress.setStartedAt(LocalDateTime.now());
                newProgress.setLastReadAt(LocalDateTime.now());

                readingMapper.insertReadingProgress(newProgress);
                existingProgress = newProgress;
            }

            ReadingProgressResponse response = mapToReadingProgressResponse(existingProgress, book);
            return new DataResponse<>(SUCCESS, "Progress saved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error saving reading progress for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<ReadingProgressResponse> getReadingProgress(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            ReadingProgress progress = readingMapper.findReadingProgressByUserAndBook(user.getId(), book.getId());

            if (progress == null) {
                // Create initial progress
                ReadingProgress newProgress = new ReadingProgress();
                newProgress.setUserId(user.getId());
                newProgress.setBookId(book.getId());
                newProgress.setCurrentPage(1);
                newProgress.setTotalPages(book.getTotalPages());
                newProgress.setCurrentPosition("0");
                newProgress.setPercentageCompleted(BigDecimal.ZERO);
                newProgress.setReadingTimeMinutes(0);
                newProgress.setStatus("NOT_STARTED");
                newProgress.setStartedAt(LocalDateTime.now());
                newProgress.setLastReadAt(LocalDateTime.now());

                progress = newProgress;
            }

            ReadingProgressResponse response = mapToReadingProgressResponse(progress, book);
            return new DataResponse<>(SUCCESS, "Progress retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting reading progress for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    // ============ BOOKMARK OPERATIONS ============
    @Override
    @Transactional
    public DataResponse<BookmarkResponse> addBookmark(String slug, BookmarkRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Check for duplicate bookmark
            Bookmark existing = bookmarkMapper.findBookmarkByUserBookAndPage(user.getId(), book.getId(), request.getPosition());
            if (existing != null) {
                throw new DataAlreadyExistsException();
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(user.getId());
            bookmark.setBookId(book.getId());
            bookmark.setPage(request.getPage());
            bookmark.setPosition(request.getPosition());
            bookmark.setTitle(request.getTitle());
            bookmark.setDescription(request.getDescription());
            bookmark.setColor(request.getColor() != null ? request.getColor() : "#de96be");
            bookmark.setCreatedAt(LocalDateTime.now());

            bookmarkMapper.insertBookmark(bookmark);

            BookmarkResponse response = mapToBookmarkResponse(bookmark);
            return new DataResponse<>(SUCCESS, "Bookmark added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding bookmark for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<BookmarkResponse>> getBookmarks(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            List<Bookmark> bookmarks = bookmarkMapper.findBookmarksByUserAndBook(user.getId(), book.getId());
            List<BookmarkResponse> responses = bookmarks.stream()
                    .map(this::mapToBookmarkResponse)
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Bookmarks retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting bookmarks for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<BookmarkResponse> updateBookmark(String slug, Long bookmarkId, BookmarkRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Bookmark existingBookmark = bookmarkMapper.findBookmarkById(bookmarkId);
            if (existingBookmark == null || !existingBookmark.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            // Update bookmark fields
            existingBookmark.setPage(request.getPage());
            existingBookmark.setPosition(request.getPosition());
            existingBookmark.setTitle(request.getTitle());
            existingBookmark.setDescription(request.getDescription());
            existingBookmark.setColor(request.getColor() != null ? request.getColor() : existingBookmark.getColor());

            bookmarkMapper.updateBookmark(existingBookmark);

            BookmarkResponse response = mapToBookmarkResponse(existingBookmark);
            return new DataResponse<>(SUCCESS, "Bookmark updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating bookmark: {} for book: {} user: {}", bookmarkId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DefaultResponse deleteBookmark(String slug, Long bookmarkId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Bookmark existingBookmark = bookmarkMapper.findBookmarkById(bookmarkId);
            if (existingBookmark == null || !existingBookmark.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            bookmarkMapper.deleteBookmark(bookmarkId);

            return new DefaultResponse(SUCCESS, "Bookmark deleted successfully", HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error deleting bookmark: {} for book: {} user: {}", bookmarkId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    // ============ HIGHLIGHT OPERATIONS ============
    @Override
    @Transactional
    public DataResponse<HighlightResponse> addHighlight(String slug, HighlightRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Highlight highlight = new Highlight();
            highlight.setUserId(user.getId());
            highlight.setBookId(book.getId());
            highlight.setPage(request.getPage());
            highlight.setStartPosition(request.getStartPosition());
            highlight.setEndPosition(request.getEndPosition());
            highlight.setHighlightedText(request.getHighlightedText());
            highlight.setColor(request.getColor());
            highlight.setNote(request.getNote());
            highlight.setCreatedAt(LocalDateTime.now());
            highlight.setUpdatedAt(LocalDateTime.now());

            highlightMapper.insertHighlight(highlight);

            HighlightResponse response = mapToHighlightResponse(highlight);
            return new DataResponse<>(SUCCESS, "Highlight added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding highlight for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<HighlightResponse>> getHighlights(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            List<Highlight> highlights = highlightMapper.findHighlightsByUserAndBook(user.getId(), book.getId());
            List<HighlightResponse> responses = highlights.stream()
                    .map(this::mapToHighlightResponse)
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Highlights retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting highlights for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<HighlightResponse> updateHighlight(String slug, Long highlightId, HighlightRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Highlight existingHighlight = highlightMapper.findHighlightById(highlightId);
            if (existingHighlight == null || !existingHighlight.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            // Update highlight fields
            existingHighlight.setPage(request.getPage());
            existingHighlight.setStartPosition(request.getStartPosition());
            existingHighlight.setEndPosition(request.getEndPosition());
            existingHighlight.setHighlightedText(request.getHighlightedText());
            existingHighlight.setColor(request.getColor());
            existingHighlight.setNote(request.getNote());
            existingHighlight.setUpdatedAt(LocalDateTime.now());

            highlightMapper.updateHighlight(existingHighlight);

            HighlightResponse response = mapToHighlightResponse(existingHighlight);
            return new DataResponse<>(SUCCESS, "Highlight updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating highlight: {} for book: {} user: {}", highlightId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DefaultResponse deleteHighlight(String slug, Long highlightId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Highlight existingHighlight = highlightMapper.findHighlightById(highlightId);
            if (existingHighlight == null || !existingHighlight.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            // Delete related translations first
            highlightTranslationMapper.deleteByHighlightId(highlightId);

            // Delete the highlight
            highlightMapper.deleteHighlight(highlightId);

            return new DefaultResponse(SUCCESS, "Highlight deleted successfully", HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error deleting highlight: {} for book: {} user: {}", highlightId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    // ============ NOTE OPERATIONS ============
    @Override
    @Transactional
    public DataResponse<NoteResponse> addNote(String slug, NoteRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Note note = new Note();
            note.setUserId(user.getId());
            note.setBookId(book.getId());
            note.setPage(request.getPage());
            note.setPosition(request.getPosition());
            note.setTitle(request.getTitle());
            note.setContent(request.getContent());
            note.setColor(request.getColor() != null ? request.getColor() : "#FFEB3B");
            note.setIsPrivate(request.getIsPrivate());
            note.setCreatedAt(LocalDateTime.now());
            note.setUpdatedAt(LocalDateTime.now());

            noteMapper.insertNote(note);

            NoteResponse response = mapToNoteResponse(note);
            return new DataResponse<>(SUCCESS, "Note added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding note for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<NoteResponse>> getNotes(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            List<Note> notes = noteMapper.findNotesByUserAndBook(user.getId(), book.getId());
            List<NoteResponse> responses = notes.stream()
                    .map(this::mapToNoteResponse)
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Notes retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting notes for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<NoteResponse> updateNote(String slug, Long noteId, NoteRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Note existingNote = noteMapper.findNoteById(noteId);
            if (existingNote == null || !existingNote.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            // Update note fields
            existingNote.setPage(request.getPage());
            existingNote.setPosition(request.getPosition());
            existingNote.setTitle(request.getTitle());
            existingNote.setContent(request.getContent());
            existingNote.setColor(request.getColor() != null ? request.getColor() : existingNote.getColor());
            existingNote.setIsPrivate(request.getIsPrivate());
            existingNote.setUpdatedAt(LocalDateTime.now());

            noteMapper.updateNote(existingNote);

            NoteResponse response = mapToNoteResponse(existingNote);
            return new DataResponse<>(SUCCESS, "Note updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating note: {} for book: {} user: {}", noteId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DefaultResponse deleteNote(String slug, Long noteId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Note existingNote = noteMapper.findNoteById(noteId);
            if (existingNote == null || !existingNote.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            noteMapper.deleteNote(noteId);

            return new DefaultResponse(SUCCESS, "Note deleted successfully", HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error deleting note: {} for book: {} user: {}", noteId, slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    // ============ RATING OPERATIONS ============

    @Override
    @Transactional
    public DataResponse<ReactionResponse> addOrUpdateRating(String slug, RatingRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }

            // Check if user already has rating for this book
            Reaction existingRating = reactionMapper.findRatingByUserAndBook(user.getId(), book.getId());

            Reaction savedRating;
            String message;

            if (existingRating != null) {
                // Update existing rating
                existingRating.setRating(request.getRating());
                existingRating.setUpdatedAt(LocalDateTime.now());
                reactionMapper.updateReaction(existingRating);
                savedRating = existingRating;
                message = "Rating updated successfully";
            } else {
                // Create new rating
                Reaction rating = new Reaction();
                rating.setUserId(user.getId());
                rating.setBookId(book.getId());
                rating.setReactionType("RATING");
                rating.setRating(request.getRating());
                rating.setComment(null);
                rating.setTitle(null);
                rating.setParentId(null);
                rating.setCreatedAt(LocalDateTime.now());
                rating.setUpdatedAt(LocalDateTime.now());

                reactionMapper.insertReaction(rating);
                savedRating = rating;
                message = "Rating added successfully";
            }

            ReactionResponse response = mapToReactionResponse(savedRating, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error processing rating for book: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteRating(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Find user's rating for this book
            Reaction rating = reactionMapper.findRatingByUserAndBook(user.getId(), book.getId());

            if (rating == null) {
                throw new DataNotFoundException();
            }

            // Delete the rating
            reactionMapper.deleteReaction(rating.getId());

            return new DataResponse<>(SUCCESS, "Rating deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting rating for book: {}", slug, e);
            throw e;
        }
    }

// ============ REVIEW/COMMENT OPERATIONS ============

    @Override
    public DataResponse<List<ReactionResponse>> getReviews(String slug, int page, int limit) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Get current user for reaction status
            String currentUsername = null;
            Long currentUserId = null;
            try {
                currentUsername = headerHolder.getUsername();
                if (currentUsername != null) {
                    User currentUser = userMapper.findUserByUsername(currentUsername);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            } catch (Exception e) {
                // User might not be logged in, continue without user info
            }

            // Get reviews with pagination
            int offset = (page - 1) * limit;
            List<Reaction> reviews = reactionMapper.findReviewsByBookIdWithPagination(book.getId(), offset, limit);

            final Long finalCurrentUserId = currentUserId;
            List<ReactionResponse> responses = reviews.stream()
                    .map(review -> {
                        User reviewUser = userMapper.findUserById(review.getUserId());
                        return mapToReactionResponse(review, reviewUser, book.getId(), finalCurrentUserId);
                    })
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Reviews retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting reviews for book: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ReactionResponse> addReview(String slug, ReviewRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Review content is required");
            }

            // Check if user already has review for this book
            Reaction existingReview = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());

            if (existingReview != null) {
                throw new IllegalArgumentException("You already have a review for this book. Use update endpoint to modify it.");
            }

            // Create new review
            Reaction review = new Reaction();
            review.setUserId(user.getId());
            review.setBookId(book.getId());
            review.setReactionType("COMMENT");
            review.setComment(request.getComment());
            review.setTitle(request.getTitle());
            review.setRating(null);
            review.setParentId(null);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            reactionMapper.insertReaction(review);

            ReactionResponse response = mapToReactionResponse(review, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, "Review added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding review for book: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ReactionResponse> updateReview(String slug, ReviewRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Review content is required");
            }

            // Find user's review for this book
            Reaction existingReview = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());

            if (existingReview == null) {
                throw new DataNotFoundException();
            }

            // Update review
            existingReview.setComment(request.getComment());
            existingReview.setTitle(request.getTitle());
            existingReview.setUpdatedAt(LocalDateTime.now());
            reactionMapper.updateReaction(existingReview);

            ReactionResponse response = mapToReactionResponse(existingReview, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, "Review updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating review for book: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReview(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Find user's review for this book
            Reaction review = reactionMapper.findReviewByUserAndBook(user.getId(), book.getId());

            if (review == null) {
                throw new DataNotFoundException();
            }

            // Delete the review and all its replies/feedback
            reactionMapper.deleteReactionAndReplies(review.getId());

            return new DataResponse<>(SUCCESS, "Review deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting review for book: {}", slug, e);
            throw e;
        }
    }

// ============ REPLY OPERATIONS ============

    @Override
    @Transactional
    public DataResponse<ReactionResponse> addReply(String slug, Long parentId, ReplyRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Reply content is required");
            }

            // Verify parent review/comment exists
            Reaction parentReaction = reactionMapper.findReactionById(parentId);
            if (parentReaction == null) {
                throw new DataNotFoundException();
            }

            // User cannot reply to their own review/comment
            if (parentReaction.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot reply to your own review/comment");
            }

            // Verify parent belongs to the same book
            if (!parentReaction.getBookId().equals(book.getId())) {
                throw new IllegalArgumentException("Parent reaction does not belong to this book");
            }

            // Create reply
            Reaction reply = new Reaction();
            reply.setUserId(user.getId());
            reply.setBookId(book.getId());
            reply.setReactionType("COMMENT");
            reply.setComment(request.getComment());
            reply.setTitle(null);
            reply.setParentId(parentId);
            reply.setRating(null);
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());

            reactionMapper.insertReaction(reply);

            ReactionResponse response = mapToReactionResponse(reply, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, "Reply added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding reply for review: {}", parentId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ReactionResponse> updateReply(String slug, Long replyId, ReplyRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Reply content is required");
            }

            // Get the reply to verify ownership
            Reaction reply = reactionMapper.findReactionById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            // Verify it's a reply (has parentId)
            if (reply.getParentId() == null) {
                throw new IllegalArgumentException("This is not a reply");
            }

            // Check if user owns the reply
            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            // Update the reply
            reply.setComment(request.getComment());
            reply.setUpdatedAt(LocalDateTime.now());
            reactionMapper.updateReaction(reply);

            ReactionResponse response = mapToReactionResponse(reply, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, "Reply updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating reply: {}", replyId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReply(String slug, Long replyId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Get the reply to verify ownership
            Reaction reply = reactionMapper.findReactionById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            // Verify it's a reply (has parentId)
            if (reply.getParentId() == null) {
                throw new IllegalArgumentException("This is not a reply");
            }

            // Check if user owns the reply
            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            // Delete the reply and its nested replies
            reactionMapper.deleteReactionAndReplies(replyId);

            return new DataResponse<>(SUCCESS, "Reply deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting reply: {}", replyId, e);
            throw e;
        }
    }

// ============ FEEDBACK OPERATIONS (HELPFUL/NOT_HELPFUL) ============

    @Override
    @Transactional
    public DataResponse<ReactionResponse> addOrUpdateFeedback(String slug, Long reviewId, FeedbackRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Validate feedback type
            String feedbackType = request.getType().toUpperCase();
            if (!feedbackType.equals("HELPFUL") && !feedbackType.equals("NOT_HELPFUL")) {
                throw new IllegalArgumentException("Feedback type must be HELPFUL or NOT_HELPFUL");
            }

            // Verify review exists
            Reaction review = reactionMapper.findReactionById(reviewId);
            if (review == null) {
                throw new DataNotFoundException();
            }

            // User cannot give feedback to their own review
            if (review.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot give feedback to your own review");
            }

            // Check if user already gave feedback on this review
            Reaction existingFeedback = reactionMapper.findFeedbackByUserAndReview(user.getId(), reviewId);

            Reaction savedFeedback;
            String message;

            if (existingFeedback != null) {
                // Update existing feedback
                existingFeedback.setReactionType(feedbackType);
                existingFeedback.setUpdatedAt(LocalDateTime.now());
                reactionMapper.updateReaction(existingFeedback);
                savedFeedback = existingFeedback;
                message = "Feedback updated successfully";
            } else {
                // Create new feedback
                Reaction feedback = new Reaction();
                feedback.setUserId(user.getId());
                feedback.setBookId(book.getId());
                feedback.setReactionType(feedbackType);
                feedback.setComment(null);
                feedback.setTitle(null);
                feedback.setRating(null);
                feedback.setParentId(reviewId);
                feedback.setCreatedAt(LocalDateTime.now());
                feedback.setUpdatedAt(LocalDateTime.now());

                reactionMapper.insertReaction(feedback);
                savedFeedback = feedback;
                message = "Feedback added successfully";
            }

            ReactionResponse response = mapToReactionResponse(savedFeedback, user, book.getId(), user.getId());
            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error processing feedback for review: {}", reviewId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteFeedback(String slug, Long reviewId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            // Find user's feedback for this review
            Reaction feedback = reactionMapper.findFeedbackByUserAndReview(user.getId(), reviewId);

            if (feedback == null) {
                throw new DataNotFoundException();
            }

            // Delete the feedback
            reactionMapper.deleteReaction(feedback.getId());

            return new DataResponse<>(SUCCESS, "Feedback deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting feedback for review: {}", reviewId, e);
            throw e;
        }
    }

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

    @Override
    public DataResponse<TranslationResponse> translateText(String slug, TranslationRequest request) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Use Microsoft Translator API (free tier)
            String translatedText = translatorUtil.translate(
                    request.getText(),
                    request.getSourceLanguage(),
                    request.getTargetLanguage()
            );

            TranslationResponse response = new TranslationResponse();
            response.setOriginalText(request.getText());
            response.setTranslatedText(translatedText);
            response.setSourceLanguage(request.getSourceLanguage());
            response.setTargetLanguage(request.getTargetLanguage());
            response.setConfidenceScore(0.85); // Estimated confidence
            response.setTranslationProvider("Microsoft Translator");

            return new DataResponse<>(SUCCESS, "Translation completed successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error translating text for book: {}", slug, e);
            throw new RuntimeException("Translation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DataResponse<TranslatedHighlightResponse> translateHighlight(String slug, TranslateHighlightRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Highlight highlight = highlightMapper.findHighlightById(request.getHighlightId());

            if (user == null || highlight == null || !highlight.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            String translatedText = translatorUtil.translate(
                    highlight.getHighlightedText(),
                    null,
                    request.getTargetLanguage()
            );

            // Save translation for future use
            saveHighlightTranslation(highlight.getId(), request.getTargetLanguage(), translatedText);

            TranslatedHighlightResponse response = new TranslatedHighlightResponse();
            response.setHighlightId(highlight.getId());
            response.setOriginalText(highlight.getHighlightedText());
            response.setTranslatedText(translatedText);
            response.setTargetLanguage(request.getTargetLanguage());
            response.setTranslatedAt(LocalDateTime.now());
            response.setIsSaved(true);

            return new DataResponse<>(SUCCESS, "Highlight translated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error translating highlight: {}", request.getHighlightId(), e);
            throw new RuntimeException("Highlight translation failed: " + e.getMessage());
        }
    }

    // ============ TTS & AUDIO OPERATIONS ============
    @Override
    public DataResponse<TTSResponse> generateTextToSpeech(String slug, TTSRequest request) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Use Microsoft Edge TTS (free)
            byte[] audioData = ttsUtil.generateSpeech(
                    request.getText(),
                    request.getVoice(),
                    request.getSpeed(),
                    request.getPitch()
            );

            // Save audio file
            String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
            String audioPath = saveTTSFile(fileName, audioData);

            TTSResponse response = new TTSResponse();
            response.setAudioUrl(audioPath);
            response.setText(request.getText());
            response.setVoice(request.getVoice());
            response.setDuration(estimateAudioDuration(request.getText()));
            response.setFormat("mp3");
            response.setFileSize((long) audioData.length);
            response.setGeneratedAt(LocalDateTime.now());
            response.setExpiresAt(LocalDateTime.now().plusDays(1).toString());

            return new DataResponse<>(SUCCESS, "Text-to-speech generated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error generating TTS for book: {}", slug, e);
            throw new RuntimeException("TTS generation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DataResponse<AudioSyncResponse> syncAudioWithText(String slug, AudioSyncRequest request) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Create or update sync point
            AudioSync sync = new AudioSync();
            sync.setBookId(book.getId());
            sync.setPage(request.getPage());
            sync.setTextPosition(request.getTextPosition());
            sync.setAudioTimestamp(request.getAudioTimestamp());
            sync.setCreatedAt(LocalDateTime.now());

            // Save sync point (implementation depends on your audio sync strategy)
            Long syncId = saveAudioSync(sync);

            // Get all sync points for this page
            List<AudioSyncResponse.SyncPoint> syncPoints = getSyncPointsForPage(book.getId(), request.getPage());

            AudioSyncResponse response = new AudioSyncResponse();
            response.setSyncId(syncId);
            response.setPage(request.getPage());
            response.setSyncPoints(syncPoints);
            response.setStatus("SYNCED");

            return new DataResponse<>(SUCCESS, "Audio sync updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error syncing audio for book: {}", slug, e);
            throw new RuntimeException("Audio sync failed: " + e.getMessage());
        }
    }

    // ============ HELPER METHODS ============
    private ReadingResponse getGuestReadingResponse(Book book) {
        ReadingResponse response = new ReadingResponse();

        // Basic book information
        response.setBookId(book.getId());
        response.setTitle(book.getTitle());
        response.setSlug(book.getSlug());
        response.setFileUrl(book.getFileUrl());
        response.setTotalPages(book.getTotalPages());
        response.setCurrentPage(1);
        response.setCurrentPosition("0");
        response.setPercentageCompleted(BigDecimal.ZERO);
        response.setReadingTimeMinutes(0);
        response.setStatus("GUEST_READING");
        response.setIsFavorite(false);
        response.setSessionId(null);

        return response;
    }

    private ReadingResponse getReadingResponse(Book book, ReadingProgress existingProgress, ReadingSession session) {
        ReadingResponse readingResponse = new ReadingResponse();
        readingResponse.setBookId(book.getId());
        readingResponse.setTitle(book.getTitle());
        readingResponse.setSlug(book.getSlug());
        readingResponse.setFileUrl(book.getFileUrl());
        readingResponse.setCurrentPage(existingProgress.getCurrentPage());
        readingResponse.setTotalPages(existingProgress.getTotalPages());
        readingResponse.setCurrentPosition(existingProgress.getCurrentPosition());
        readingResponse.setPercentageCompleted(existingProgress.getPercentageCompleted());
        readingResponse.setReadingTimeMinutes(existingProgress.getReadingTimeMinutes());
        readingResponse.setStatus(existingProgress.getStatus());
        readingResponse.setIsFavorite(existingProgress.getIsFavorite());
        readingResponse.setSessionId(session.getId());
        return readingResponse;
    }

    private ReadingProgressResponse mapToReadingProgressResponse(ReadingProgress progress, Book book) {
        ReadingProgressResponse response = new ReadingProgressResponse();
        response.setId(progress.getId());
        response.setBookId(progress.getBookId());
        response.setBookTitle(book.getTitle());
        response.setCurrentPage(progress.getCurrentPage());
        response.setTotalPages(progress.getTotalPages());
        response.setCurrentPosition(progress.getCurrentPosition());
        response.setPercentageCompleted(progress.getPercentageCompleted());
        response.setReadingTimeMinutes(progress.getReadingTimeMinutes());
        response.setStatus(progress.getStatus());
        response.setStartedAt(progress.getStartedAt());
        response.setLastReadAt(progress.getLastReadAt());
        return response;
    }

    private BookmarkResponse mapToBookmarkResponse(Bookmark bookmark) {
        BookmarkResponse response = new BookmarkResponse();
        response.setId(bookmark.getId());
        response.setBookId(bookmark.getBookId());
        response.setPage(bookmark.getPage());
        response.setPosition(bookmark.getPosition());
        response.setTitle(bookmark.getTitle());
        response.setDescription(bookmark.getDescription());
        response.setColor(bookmark.getColor());
        response.setCreatedAt(bookmark.getCreatedAt());
        return response;
    }

    private HighlightResponse mapToHighlightResponse(Highlight highlight) {
        HighlightResponse response = new HighlightResponse();
        response.setId(highlight.getId());
        response.setBookId(highlight.getBookId());
        response.setPage(highlight.getPage());
        response.setStartPosition(highlight.getStartPosition());
        response.setEndPosition(highlight.getEndPosition());
        response.setHighlightedText(highlight.getHighlightedText());
        response.setColor(highlight.getColor());
        response.setNote(highlight.getNote());
        response.setCreatedAt(highlight.getCreatedAt());
        response.setUpdatedAt(highlight.getUpdatedAt());
        return response;
    }

    private NoteResponse mapToNoteResponse(Note note) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setBookId(note.getBookId());
        response.setPage(note.getPage());
        response.setPosition(note.getPosition());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setColor(note.getColor());
        response.setIsPrivate(note.getIsPrivate());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
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
//                response.setTotalRatings(stats.getTotalRatings() != null ? stats.getTotalRatings() : 0L);
//                response.setTotalAngry(stats.getTotalAngry() != null ? stats.getTotalAngry() : 0L);
//                response.setTotalLikes(stats.getTotalLikes() != null ? stats.getTotalLikes() : 0L);
//                response.setTotalLoves(stats.getTotalLoves() != null ? stats.getTotalLoves() : 0L);
//                response.setTotalDislikes(stats.getTotalDislikes() != null ? stats.getTotalDislikes() : 0L);
//                response.setTotalSad(stats.getTotalSad() != null ? stats.getTotalSad() : 0L);
//                response.setTotalComments(stats.getTotalComments() != null ? stats.getTotalComments() : 0L);
//                response.setAverageRating(stats.getAverageRating() != null ? stats.getAverageRating() : 0.0);
            } else {
                setDefaultStats(response);
            }

            if (currentUserId != null) {
                String userReactionType = reactionMapper.getUserReactionType(currentUserId, bookId);
//                response.setUserHasReacted(userReactionType != null);
//                response.setUserReactionType(userReactionType);
            } else {
//                response.setUserHasReacted(false);
//                response.setUserReactionType(null);
            }
        } catch (Exception e) {
            log.warn("Failed to get reaction stats: {}", e.getMessage());
            setDefaultStats(response);
//            response.setUserHasReacted(false);
//            response.setUserReactionType(null);
        }

        return response;
    }

    private void setDefaultStats(ReactionResponse response) {
//        response.setTotalRatings(0L);
//        response.setTotalAngry(0L);
//        response.setTotalLikes(0L);
//        response.setTotalLoves(0L);
//        response.setTotalDislikes(0L);
//        response.setTotalSad(0L);
//        response.setTotalComments(0L);
//        response.setAverageRating(0.0);
    }

    private void saveHighlightTranslation(Long highlightId, String targetLanguage, String translatedText) {
        try {
            // Check if translation already exists
            HighlightTranslation existingTranslation = highlightTranslationMapper
                    .findByHighlightIdAndLanguage(highlightId, targetLanguage);

            if (existingTranslation != null) {
                // Update existing translation
                existingTranslation.setTranslatedText(translatedText);
                existingTranslation.setUpdatedAt(LocalDateTime.now());
                highlightTranslationMapper.updateTranslation(existingTranslation);
            } else {
                // Create new translation
                HighlightTranslation translation = new HighlightTranslation();
                translation.setHighlightId(highlightId);
                translation.setTargetLanguage(targetLanguage);
                translation.setTranslatedText(translatedText);
                translation.setCreatedAt(LocalDateTime.now());
                highlightTranslationMapper.insertTranslation(translation);
            }
        } catch (Exception e) {
            log.warn("Failed to save highlight translation: {}", e.getMessage());
            // Don't throw exception as this is optional functionality
        }
    }

    private String saveTTSFile(String fileName, byte[] audioData) {
        try {
            // Create uploads directory if it doesn't exist
            String uploadDir = "uploads/tts/";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Save file
            String filePath = uploadDir + fileName;
            File file = new File(filePath);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(audioData);
            }

            // Return relative URL path
            return "/api/files/tts/" + fileName;

        } catch (IOException e) {
            log.error("Failed to save TTS file: {}", e.getMessage());
            throw new RuntimeException("Failed to save audio file: " + e.getMessage());
        }
    }

    private Double estimateAudioDuration(String text) {
        // Simple estimation: average reading speed is about 150-200 words per minute
        // For TTS, it's usually slower, around 150 words per minute
        if (text == null || text.trim().isEmpty()) {
            return (double) 0L;
        }

        // Count words (simple split by spaces)
        String[] words = text.trim().split("\\s+");
        int wordCount = words.length;

        // Calculate duration in seconds (150 words per minute)
        long durationSeconds = (wordCount * 60L) / 150;

        // Minimum 1 second
        return (double) Math.max(durationSeconds, 1L);
    }

    private Long saveAudioSync(AudioSync sync) {
        try {
            // Check if sync point already exists for this position
            AudioSync existingSync = audioSyncMapper.findByBookPageAndPosition(
                    sync.getBookId(), sync.getPage(), Integer.valueOf(sync.getTextPosition()));

            if (existingSync != null) {
                // Update existing sync point
                existingSync.setAudioTimestamp(sync.getAudioTimestamp());
                audioSyncMapper.updateAudioSync(existingSync);
                return existingSync.getId();
            } else {
                // Create new sync point
                audioSyncMapper.insertAudioSync(sync);
                return sync.getId();
            }
        } catch (Exception e) {
            log.error("Failed to save audio sync: {}", e.getMessage());
            throw new RuntimeException("Failed to save audio sync: " + e.getMessage());
        }
    }

    private List<AudioSyncResponse.SyncPoint> getSyncPointsForPage(Long bookId, Integer page) {
        try {
            List<AudioSync> syncs = audioSyncMapper.findByBookIdAndPage(bookId, page);

            return syncs.stream().map(sync -> {
                AudioSyncResponse.SyncPoint syncPoint = new AudioSyncResponse.SyncPoint();
                syncPoint.setTextPosition(sync.getTextPosition());
                syncPoint.setAudioTimestamp(sync.getAudioTimestamp());
                return syncPoint;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get sync points: {}", e.getMessage());
            return new ArrayList<>();
        }
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