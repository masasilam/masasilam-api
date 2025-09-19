package com.naskah.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naskah.demo.exception.custom.DataAlreadyExistsException;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.BookService;
import com.naskah.demo.util.ai.OpenAIUtil;
import com.naskah.demo.util.file.BookMetadata;
import com.naskah.demo.util.file.FileStorageResult;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import com.naskah.demo.util.translation.MicrosoftTranslatorUtil;
import com.naskah.demo.util.tts.MicrosoftTTSUtil;
import com.naskah.demo.util.voice.VoiceCommandProcessor;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.naskah.demo.util.file.FileUtil.sanitizeFilename;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final AuthorMapper authorMapper;
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
    private final VoiceNoteMapper voiceNoteMapper;
    private final CollaborativeNoteMapper collaborativeNoteMapper;

    // Utility services
    private final MicrosoftTranslatorUtil translatorUtil;
    private final MicrosoftTTSUtil ttsUtil;
    private final OpenAIUtil openAIUtil;

    private static final String SUCCESS = "Success";
    private static final String UNAUTHORIZED_MESSAGE = "User not authenticated";

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

            // Declare variables properly
            BookMetadata metadata = FileUtil.extractBookMetadata(request.getBookFile(), language.getId());
            FileStorageResult coverResult = FileUtil.saveAndUploadBookCover(request.getCoverImage(), request.getTitle());
            FileStorageResult bookResult = FileUtil.saveAndUploadBookFile(request.getBookFile(), request.getTitle());

            Book book = new Book();
            book.setTitle(request.getTitle());
            book.setSlug(sanitizeFilename(request.getTitle()));
            book.setSubtitle(request.getSubtitle());
            book.setSeriesId(request.getSeriesId());
            book.setSeriesOrder(request.getSeriesOrder());
            book.setIsbn(request.getIsbn());
            book.setPublicationYear(request.getPublicationYear());
            book.setPublisher(request.getPublisher());
            book.setLanguageId(language.getId());
            book.setDescription(request.getDescription());
            book.setSummary(request.getSummary());
            book.setCoverImageUrl(coverResult.getCloudUrl());
            book.setFileUrl(bookResult.getCloudUrl());
            book.setFilePath(bookResult.getLocalPath());
            book.setCoverImagePath(coverResult.getLocalPath());
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
            book.setAverageRating(BigDecimal.ZERO);
            book.setTotalReviews(0);
            book.setIsActive(true);
            book.setIsFeatured(false);
            book.setPublishedAt(request.getPublishedAt());
            book.setCategory(request.getCategory());
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());

            bookMapper.insertBook(book);

            for (Long genreId : request.getGenreIds()) {
                bookMapper.insertBookGenre(book.getId(), genreId);
            }
            for (Tag tag : request.getTagIds()) {
                bookMapper.insertBookTag(book.getId(), tag.getId());
            }

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
                    newAuthor.setSlug(sanitizeFilename(authorRequest.getName()));
                    newAuthor.setBirthDate(authorRequest.getBirthDate());
                    newAuthor.setDeathDate(authorRequest.getDeathDate());
                    newAuthor.setBirthPlace(authorRequest.getBirthPlace());
                    newAuthor.setNationality(authorRequest.getNationality());
                    newAuthor.setBiography(authorRequest.getBiography());

                    if (authorRequest.getPhoto() != null) {
                        FileStorageResult authorPhotoResult = FileUtil.saveAndUploadAuthorPhoto(authorRequest.getPhoto(), authorRequest.getName());
                        newAuthor.setPhotoUrl(authorPhotoResult.getCloudUrl());
                        newAuthor.setPhotoPath(authorPhotoResult.getLocalPath());
                    }

                    newAuthor.setTotalBooks(1);
                    newAuthor.setCreatedAt(LocalDateTime.now());
                    newAuthor.setUpdatedAt(LocalDateTime.now());

                    authorMapper.insertAuthor(newAuthor);

                    bookMapper.insertBookAuthor(book.getId(), newAuthor.getId());
                }
            }

            BookResponse data = bookMapper.getBookDetailBySlug(book.getSlug());
            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, HttpStatus.CREATED.value(), data);

        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan file", e);
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

//    @Override
//    public DataResponse<Book> update(String id, Book book, MultipartFile file) throws IOException {
//        try {
//            Book existingEbook = bookMapper.getDetailEbook(id);
//            if (existingEbook == null) {
//                throw new NotFoundException();
//            }
//
//            book.setId(id);
//
//            if (file != null && !file.isEmpty()) {
//                Path oldFilePath = Paths.get(existingEbook.getFilePath());
//                if (Files.exists(oldFilePath)) {
//                    Files.delete(oldFilePath);
//                }
//
//                String filePath = fileUtil.saveFile(file, id);
//                book.setFilePath(filePath);
//            } else {
//                book.setFilePath(existingEbook.getFilePath());
//            }
//
//            bookMapper.updateEbook(book);
//            Book data = bookMapper.getDetailEbook(id);
//            if (data != null) {
//                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_UPDATED, HttpStatus.OK.value(), data);
//            } else {
//                throw new NotFoundException();
//            }
//
//        } catch (Exception e) {
//            log.error("Error when update ebook", e);
//            throw e;
//        }
//    }
//
//    @Override
//    public DefaultResponse delete(String id) throws IOException {
//        try {
//            Book ebook = bookMapper.getDetailEbook(id);
//            if (ebook != null) {
//                Path filePath = Paths.get(ebook.getFilePath());
//                if (Files.exists(filePath)) {
//                    Files.delete(filePath);
//                }
//                bookMapper.deleteEbook(id);
//                return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());
//            } else {
//                throw new NotFoundException();
//            }
//        } catch (Exception e) {
//            log.error("Error when delete ebook", e);
//            throw e;
//        }
//    }


 //    ============ NEW FEATURE IMPLEMENTATIONS ============

    // 1. Save Reading Progress
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

    // 2. Get Reading Progress
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

    // 3. Add Bookmark
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
            Bookmark existing = bookmarkMapper.findBookmarkByUserBookAndPage(user.getId(), book.getId(), request.getPage());
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
            bookmark.setColor(request.getColor() != null ? request.getColor() : "#FFD700");
            bookmark.setCreatedAt(LocalDateTime.now());

            bookmarkMapper.insertBookmark(bookmark);

            BookmarkResponse response = mapToBookmarkResponse(bookmark);
            return new DataResponse<>(SUCCESS, "Bookmark added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding bookmark for book: {} user: {}", slug, headerHolder.getUsername(), e);
            throw e;
        }
    }

    // 4. Get Bookmarks
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

    // 5. Search in Book
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

    // 6. Add Highlight
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

    // 7. Get Highlights
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

    // 8. Add Note
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

    // 9. Get Notes
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

    // 10. Export Highlights and Notes
    @Override
    public DataResponse<ExportResponse> exportHighlightsAndNotes(String slug, String format) {
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
            List<Note> notes = noteMapper.findNotesByUserAndBook(user.getId(), book.getId());

            // Generate export file
            String fileName = sanitizeFilename(book.getTitle()) + "_highlights_notes." + format.toLowerCase();
            String exportContent = generateExportContent(highlights, notes, book, format);
            String filePath = saveExportFile(fileName, exportContent, format);

            ExportResponse response = new ExportResponse();
            response.setFileName(fileName);
            response.setDownloadUrl(filePath);
            response.setFormat(format);
            response.setFileSize((long) exportContent.length());
            response.setTotalHighlights(highlights.size());
            response.setTotalNotes(notes.size());
            response.setGeneratedAt(LocalDateTime.now());
            response.setExpiresAt(LocalDateTime.now().plusDays(7).toString());

            return new DataResponse<>(SUCCESS, "Export generated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error exporting highlights and notes for book: {}", slug, e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }

    // 11. Translate Text
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

//    // 12. Get Dual Language View
//    @Override
//    public DataResponse<DualLanguageResponse> getDualLanguageView(String slug, String targetLanguage, int page) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get page content
//            String originalText = getPageContent(book.getFileUrl(), page);
//
//            // Check cache for existing translation
//            String translatedText = getCachedTranslation(book.getId(), page, targetLanguage);
//            boolean isCached = translatedText != null;
//
//            if (!isCached) {
//                // Translate and cache
//                translatedText = translatorUtil.translate(originalText, null, targetLanguage);
//                cacheTranslation(book.getId(), page, targetLanguage, translatedText);
//            }
//
//            DualLanguageResponse response = new DualLanguageResponse();
//            response.setPage(page);
//            response.setOriginalText(originalText);
//            response.setTranslatedText(translatedText);
//            response.setSourceLanguage(getBookLanguage(book));
//            response.setTargetLanguage(targetLanguage);
//            response.setIsTranslationCached(isCached);
//
//            return new DataResponse<>(SUCCESS, "Dual language view loaded", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error getting dual language view for book: {}", slug, e);
//            throw new RuntimeException("Dual language view failed: " + e.getMessage());
//        }
//    }
//
//    // 13. Translate Highlight
//    @Override
//    @Transactional
//    public DataResponse<TranslatedHighlightResponse> translateHighlight(String slug, TranslateHighlightRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Highlight highlight = highlightMapper.findHighlightById(request.getHighlightId());
//
//            if (user == null || highlight == null || !highlight.getUserId().equals(user.getId())) {
//                throw new DataNotFoundException();
//            }
//
//            String translatedText = translatorUtil.translate(
//                    highlight.getHighlightedText(),
//                    null,
//                    request.getTargetLanguage()
//            );
//
//            // Save translation for future use
//            saveHighlightTranslation(highlight.getId(), request.getTargetLanguage(), translatedText);
//
//            TranslatedHighlightResponse response = new TranslatedHighlightResponse();
//            response.setHighlightId(highlight.getId());
//            response.setOriginalText(highlight.getHighlightedText());
//            response.setTranslatedText(translatedText);
//            response.setTargetLanguage(request.getTargetLanguage());
//            response.setTranslatedAt(LocalDateTime.now());
//            response.setIsSaved(true);
//
//            return new DataResponse<>(SUCCESS, "Highlight translated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error translating highlight: {}", request.getHighlightId(), e);
//            throw new RuntimeException("Highlight translation failed: " + e.getMessage());
//        }
//    }
//
//    // 14. Add Reaction
//    @Override
//    @Transactional
//    public DataResponse<ReactionResponse> addReaction(String slug, ReactionRequest request) {
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
//            // Check if user already reacted
//            Reaction existingReaction = reactionMapper.findReactionByUserAndBook(
//                    user.getId(), book.getId(), request.getPage(), request.getPosition()
//            );
//
//            if (existingReaction != null) {
//                // Update existing reaction
//                existingReaction.setReactionType(request.getReactionType());
//                existingReaction.setUpdatedAt(LocalDateTime.now());
//                reactionMapper.updateReaction(existingReaction);
//            } else {
//                // Create new reaction
//                Reaction reaction = new Reaction();
//                reaction.setUserId(user.getId());
//                reaction.setBookId(book.getId());
//                reaction.setReactionType(request.getReactionType());
//                reaction.setPage(request.getPage());
//                reaction.setPosition(request.getPosition());
//                reaction.setCreatedAt(LocalDateTime.now());
//
//                reactionMapper.insertReaction(reaction);
//                existingReaction = reaction;
//            }
//
//            ReactionResponse response = mapToReactionResponse(existingReaction, user.getUsername());
//            return new DataResponse<>(SUCCESS, "Reaction added successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding reaction for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    // 15. Get Discussions & Add Discussion
//    @Override
//    public DataResponse<List<DiscussionResponse>> getDiscussions(String slug, int page, int limit) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            int offset = (page - 1) * limit;
//            List<Discussion> discussions = discussionMapper.findDiscussionsByBook(book.getId(), offset, limit);
//            List<DiscussionResponse> responses = discussions.stream()
//                    .map(this::mapToDiscussionResponse)
//                    .collect(Collectors.toList());
//
//            return new DataResponse<>(SUCCESS, "Discussions retrieved successfully", HttpStatus.OK.value(), responses);
//
//        } catch (Exception e) {
//            log.error("Error getting discussions for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public DataResponse<DiscussionResponse> addDiscussion(String slug, DiscussionRequest request) {
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
//            Discussion discussion = new Discussion();
//            discussion.setBookId(book.getId());
//            discussion.setUserId(user.getId());
//            discussion.setTitle(request.getTitle());
//            discussion.setContent(request.getContent());
//            discussion.setPage(request.getPage());
//            discussion.setPosition(request.getPosition());
//            discussion.setParentId(request.getParentId());
//            discussion.setCreatedAt(LocalDateTime.now());
//
//            discussionMapper.insertDiscussion(discussion);
//
//            DiscussionResponse response = mapToDiscussionResponse(discussion);
//            return new DataResponse<>(SUCCESS, "Discussion added successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding discussion for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    // 16. Generate Text-to-Speech
//    @Override
//    public DataResponse<TTSResponse> generateTextToSpeech(String slug, TTSRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Use Microsoft Edge TTS (free)
//            byte[] audioData = ttsUtil.generateSpeech(
//                    request.getText(),
//                    request.getVoice(),
//                    request.getSpeed(),
//                    request.getPitch()
//            );
//
//            // Save audio file
//            String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
//            String audioPath = saveTTSFile(fileName, audioData);
//
//            TTSResponse response = new TTSResponse();
//            response.setAudioUrl(audioPath);
//            response.setText(request.getText());
//            response.setVoice(request.getVoice());
//            response.setDuration(estimateAudioDuration(request.getText()));
//            response.setFormat("mp3");
//            response.setFileSize((long) audioData.length);
//            response.setGeneratedAt(LocalDateTime.now());
//            response.setExpiresAt(LocalDateTime.now().plusDays(1).toString());
//
//            return new DataResponse<>(SUCCESS, "Text-to-speech generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error generating TTS for book: {}", slug, e);
//            throw new RuntimeException("TTS generation failed: " + e.getMessage());
//        }
//    }
//
//    // 17. Sync Audio with Text
//    @Override
//    @Transactional
//    public DataResponse<AudioSyncResponse> syncAudioWithText(String slug, AudioSyncRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Create or update sync point
//            AudioSync sync = new AudioSync();
//            sync.setBookId(book.getId());
//            sync.setPage(request.getPage());
//            sync.setTextPosition(request.getTextPosition());
//            sync.setAudioTimestamp(request.getAudioTimestamp());
//            sync.setCreatedAt(LocalDateTime.now());
//
//            // Save sync point (implementation depends on your audio sync strategy)
//            Long syncId = saveAudioSync(sync);
//
//            // Get all sync points for this page
//            List<AudioSyncResponse.SyncPoint> syncPoints = getSyncPointsForPage(book.getId(), request.getPage());
//
//            AudioSyncResponse response = new AudioSyncResponse();
//            response.setSyncId(syncId);
//            response.setPage(request.getPage());
//            response.setSyncPoints(syncPoints);
//            response.setStatus("SYNCED");
//
//            return new DataResponse<>(SUCCESS, "Audio sync updated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error syncing audio for book: {}", slug, e);
//            throw new RuntimeException("Audio sync failed: " + e.getMessage());
//        }
//    }
//
//    // 18. Voice Notes
//    @Override
//    @Transactional
//    public DataResponse<VoiceNoteResponse> addVoiceNote(String slug, MultipartFile audioFile, int page, String position) {
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
//            // Save audio file
//            String fileName = "voice_note_" + System.currentTimeMillis() + ".wav";
//            String audioPath = saveVoiceNoteFile(audioFile, fileName);
//
//            // Generate transcription (optional, using speech-to-text)
//            String transcription = generateTranscription(audioFile);
//
//            VoiceNote voiceNote = new VoiceNote();
//            voiceNote.setUserId(user.getId());
//            voiceNote.setBookId(book.getId());
//            voiceNote.setPage(page);
//            voiceNote.setPosition(position);
//            voiceNote.setAudioUrl(audioPath);
//            voiceNote.setDuration(getAudioDuration(audioFile));
//            voiceNote.setTranscription(transcription);
//            voiceNote.setCreatedAt(LocalDateTime.now());
//
//            voiceNoteMapper.insertVoiceNote(voiceNote);
//
//            VoiceNoteResponse response = mapToVoiceNoteResponse(voiceNote);
//            return new DataResponse<>(SUCCESS, "Voice note added successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding voice note for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    public DataResponse<List<VoiceNoteResponse>> getVoiceNotes(String slug) {
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
//            List<VoiceNote> voiceNotes = voiceNoteMapper.findVoiceNotesByUserAndBook(user.getId(), book.getId());
//            List<VoiceNoteResponse> responses = voiceNotes.stream()
//                    .map(this::mapToVoiceNoteResponse)
//                    .collect(Collectors.toList());
//
//            return new DataResponse<>(SUCCESS, "Voice notes retrieved successfully", HttpStatus.OK.value(), responses);
//
//        } catch (Exception e) {
//            log.error("Error getting voice notes for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    // 19. Smart Vocabulary Builder
//    @Override
//    public DataResponse<VocabularyResponse> extractVocabulary(String slug, VocabularyRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Extract vocabulary from specified pages
//            String bookContent = getPageRangeContent(book.getFileUrl(), request.getStartPage(), request.getEndPage());
//            List<VocabularyResponse.VocabularyWord> words = extractVocabularyWords(bookContent, request);
//
//            VocabularyResponse response = new VocabularyResponse();
//            response.setTotalWords(words.size());
//            response.setDifficultyLevel(request.getDifficultyLevel());
//            response.setWords(words);
//
//            return new DataResponse<>(SUCCESS, "Vocabulary extracted successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error extracting vocabulary for book: {}", slug, e);
//            throw new RuntimeException("Vocabulary extraction failed: " + e.getMessage());
//        }
//    }
//
//    // 20. AI Smart Summary per Chapter
//    @Override
//    public DataResponse<SummaryResponse> generateChapterSummary(String slug, SummaryRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get chapter content
//            String chapterContent = getChapterContent(book.getFileUrl(), request.getChapter());
//
//            // Generate summary using OpenAI
//            String summary = openAIUtil.generateSummary(chapterContent, request.getSummaryType(), request.getMaxLength());
//            List<String> keyPoints = openAIUtil.extractKeyPoints(chapterContent);
//
//            SummaryResponse response = new SummaryResponse();
//            response.setChapter(request.getChapter());
//            response.setChapterTitle(getChapterTitle(book.getFileUrl(), request.getChapter()));
//            response.setSummary(summary);
//            response.setSummaryType(request.getSummaryType());
//            response.setKeyPoints(keyPoints);
//            response.setWordCount(summary.split("\\s+").length);
//            response.setGeneratedAt(LocalDateTime.now());
//
//            return new DataResponse<>(SUCCESS, "Chapter summary generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error generating summary for book: {} chapter: {}", slug, request.getChapter(), e);
//            throw new RuntimeException("Summary generation failed: " + e.getMessage());
//        }
//    }
//
//    // 21. AI Q&A on Book Content
//    @Override
//    public DataResponse<QAResponse> askQuestionAboutBook(String slug, QARequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get relevant context
//            String context = getRelevantContext(book.getFileUrl(), request.getQuestion(), request.getContextPage());
//
//            // Generate answer using OpenAI
//            String answer = openAIUtil.answerQuestion(request.getQuestion(), context);
//            List<QAResponse.Reference> references = findReferences(book.getFileUrl(), request.getQuestion());
//
//            QAResponse response = new QAResponse();
//            response.setQuestion(request.getQuestion());
//            response.setAnswer(answer);
//            response.setReferences(references);
//            response.setConfidenceScore(0.85); // Estimated confidence
//            response.setAnsweredAt(LocalDateTime.now());
//
//            return new DataResponse<>(SUCCESS, "Question answered successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error answering question for book: {}", slug, e);
//            throw new RuntimeException("Q&A failed: " + e.getMessage());
//        }
//    }
//
//    // 22. Comment & Reply Notes
//    @Override
//    @Transactional
//    public DataResponse<CommentResponse> addCommentToNote(String slug, Long noteId, CommentRequest request) {
//        try {
//            String username = headerHolder.getUsername();
//            if (username == null || username.isEmpty()) {
//                throw new UnauthorizedException();
//            }
//
//            User user = userMapper.findUserByUsername(username);
//            Note note = noteMapper.findNoteById(noteId);
//
//            if (user == null || note == null) {
//                throw new DataNotFoundException();
//            }
//
//            NoteComment comment = new NoteComment();
//            comment.setNoteId(noteId);
//            comment.setUserId(user.getId());
//            comment.setContent(request.getContent());
//            comment.setParentCommentId(request.getParentCommentId());
//            comment.setCreatedAt(LocalDateTime.now());
//
//            noteMapper.insertNoteComment(comment);
//
//            CommentResponse response = mapToCommentResponse(comment, user.getUsername());
//            return new DataResponse<>(SUCCESS, "Comment added successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error adding comment to note: {}", noteId, e);
//            throw e;
//        }
//    }
//
//    // 23. Quote Share Generator
//    @Override
//    public DataResponse<ShareQuoteResponse> generateShareableQuote(String slug, ShareQuoteRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Generate quote image
//            String imageUrl = generateQuoteImage(request, book);
//            String shareUrl = generateShareUrl(book.getSlug(), request.getPage());
//
//            ShareQuoteResponse response = new ShareQuoteResponse();
//            response.setImageUrl(imageUrl);
//            response.setText(request.getText());
//            response.setAuthorName(request.getAuthorName());
//            response.setBookTitle(book.getTitle());
//            response.setPage(request.getPage());
//            response.setTemplate(request.getTemplate());
//            response.setGeneratedAt(LocalDateTime.now());
//            response.setShareUrl(shareUrl);
//
//            return new DataResponse<>(SUCCESS, "Shareable quote generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error generating shareable quote for book: {}", slug, e);
//            throw new RuntimeException("Quote generation failed: " + e.getMessage());
//        }
//    }
//
//    // 24. Highlight Trends
//    @Override
//    public DataResponse<HighlightTrendsResponse> getHighlightTrends(String slug) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Get highlight statistics
//            List<HighlightTrendsResponse.TrendingHighlight> trendingHighlights =
//                    highlightMapper.getTrendingHighlights(book.getId());
//            List<HighlightTrendsResponse.PopularPage> popularPages =
//                    highlightMapper.getPopularPages(book.getId());
//
//            HighlightTrendsResponse response = new HighlightTrendsResponse();
//            response.setBookTitle(book.getTitle());
//            response.setTotalHighlights(highlightMapper.countHighlightsByBook(book.getId()));
//            response.setTrendingHighlights(trendingHighlights);
//            response.setPopularPages(popularPages);
//
//            return new DataResponse<>(SUCCESS, "Highlight trends retrieved successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error getting highlight trends for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    // 25. Smart Bookmark Suggestions
//    @Override
//    public DataResponse<List<BookmarkSuggestionResponse>> getBookmarkSuggestions(String slug) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Use AI to suggest important bookmark points
//            String bookContent = readBookContent(book.getFileUrl());
//            List<BookmarkSuggestionResponse> suggestions =
//                    openAIUtil.generateBookmarkSuggestions(bookContent, book.getTotalPages());
//
//            return new DataResponse<>(SUCCESS, "Bookmark suggestions generated successfully", HttpStatus.OK.value(), suggestions);
//
//        } catch (Exception e) {
//            log.error("Error generating bookmark suggestions for book: {}", slug, e);
//            throw new RuntimeException("Bookmark suggestions failed: " + e.getMessage());
//        }
//    }
//
//    // 26. Interactive Quizzes per Chapter
//    @Override
//    public DataResponse<QuizResponse> generateChapterQuiz(String slug, QuizRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            String chapterContent = getChapterContent(book.getFileUrl(), request.getChapter());
//            List<QuizResponse.QuizQuestion> questions =
//                    openAIUtil.generateQuizQuestions(chapterContent, request);
//
//            QuizResponse response = new QuizResponse();
//            response.setChapter(request.getChapter());
//            response.setChapterTitle(getChapterTitle(book.getFileUrl(), request.getChapter()));
//            response.setDifficulty(request.getDifficulty());
//            response.setQuestions(questions);
//            response.setTotalQuestions(questions.size());
//
//            return new DataResponse<>(SUCCESS, "Quiz generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error generating quiz for book: {}", slug, e);
//            throw new RuntimeException("Quiz generation failed: " + e.getMessage());
//        }
//    }
//
//    // 27. AI-powered Content Highlighting
//    @Override
//    public DataResponse<AIHighlightResponse> generateAIHighlights(String slug, AIHighlightRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            String content = getPageRangeContent(book.getFileUrl(), request.getStartPage(), request.getEndPage());
//            List<AIHighlightResponse.AIHighlight> highlights =
//                    openAIUtil.generateAIHighlights(content, request);
//
//            AIHighlightResponse response = new AIHighlightResponse();
//            response.setHighlightType(request.getHighlightType());
//            response.setTotalHighlights(highlights.size());
//            response.setHighlights(highlights);
//
//            return new DataResponse<>(SUCCESS, "AI highlights generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            throw e;
//        }
//    }
//
//    // 27. AI-powered Content Highlighting
//    @Override
//    public DataResponse<AIHighlightResponse> generateAIHighlights(String slug, AIHighlightRequest request) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            String content = getPageRangeContent(book.getFileUrl(), request.getStartPage(), request.getEndPage());
//            List<AIHighlightResponse.AIHighlight> highlights =
//                    openAIUtil.generateAIHighlights(content, request);
//
//            AIHighlightResponse response = new AIHighlightResponse();
//            response.setHighlightType(request.getHighlightType());
//            response.setTotalHighlights(highlights.size());
//            response.setHighlights(highlights);
//
//            return new DataResponse<>(SUCCESS, "AI highlights generated successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error generating AI highlights for book: {}", slug, e);
//            throw new RuntimeException("AI highlights generation failed: " + e.getMessage());
//        }
//    }
//
//    // 28. Smart Notes Tagging
//    @Override
//    @Transactional
//    public DataResponse<TaggingResponse> autoTagNotes(String slug, TaggingRequest request) {
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
//            List<Note> notes = getNotesByIds(request.getNoteIds(), user.getId(), book.getId());
//
//            // Generate tags using AI
//            Map<Long, List<String>> noteTagMapping = new HashMap<>();
//            Set<String> allGeneratedTags = new HashSet<>();
//
//            for (Note note : notes) {
//                List<String> tags = openAIUtil.generateTagsForNote(note.getContent(), note.getTitle());
//                noteTagMapping.put(note.getId(), tags);
//                allGeneratedTags.addAll(tags);
//
//                // Save tags to database
//                saveNoteTags(note.getId(), tags);
//            }
//
//            List<TaggingResponse.TagSuggestion> suggestions = generateTagSuggestions(allGeneratedTags);
//
//            TaggingResponse response = new TaggingResponse();
//            response.setProcessedNotes(notes.size());
//            response.setGeneratedTags(new ArrayList<>(allGeneratedTags));
//            response.setNoteTagMapping(noteTagMapping);
//            response.setSuggestions(suggestions);
//
//            return new DataResponse<>(SUCCESS, "Notes tagged successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error auto-tagging notes for book: {}", slug, e);
//            throw new RuntimeException("Auto-tagging failed: " + e.getMessage());
//        }
//    }
//
//    // 29. Voice-based Navigation
//    @Override
//    public DataResponse<VoiceControlResponse> processVoiceCommand(String slug, MultipartFile audioFile) {
//        try {
//            Book book = bookMapper.findBookBySlug(slug);
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            // Convert speech to text
//            String recognizedCommand = convertSpeechToText(audioFile);
//
//            // Process command using NLP
//            VoiceCommandProcessor.VoiceCommand command = parseVoiceCommand(recognizedCommand);
//            String result = executeVoiceCommand(command, book);
//
//            VoiceControlResponse response = new VoiceControlResponse();
//            response.setRecognizedCommand(recognizedCommand);
//            response.setAction(command.getAction());
//            response.setResult(result);
//            response.setSuccess(command.isValid());
//            response.setError(command.isValid() ? null : "Command not recognized");
//            response.setProcessedAt(LocalDateTime.now());
//            response.setTargetPage(command.getTargetPage());
//            response.setSearchQuery(command.getSearchQuery());
//            response.setBookmarkTitle(command.getBookmarkTitle());
//
//            return new DataResponse<>(SUCCESS, "Voice command processed successfully", HttpStatus.OK.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error processing voice command for book: {}", slug, e);
//            throw new RuntimeException("Voice command processing failed: " + e.getMessage());
//        }
//    }
//
//    // 30. Realtime Collaborative Notes
//    @Override
//    @Transactional
//    public DataResponse<CollaborativeNoteResponse> createCollaborativeNote(String slug, CollaborativeNoteRequest request) {
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
//            CollaborativeNote collabNote = new CollaborativeNote();
//            collabNote.setBookId(book.getId());
//            collabNote.setAuthorId(user.getId());
//            collabNote.setPage(request.getPage());
//            collabNote.setPosition(request.getPosition());
//            collabNote.setTitle(request.getTitle());
//            collabNote.setContent(request.getContent());
//            collabNote.setVisibility(request.getVisibility());
//            collabNote.setCreatedAt(LocalDateTime.now());
//            collabNote.setLastEditedAt(LocalDateTime.now());
//            collabNote.setLastEditedBy(user.getId());
//
//            collaborativeNoteMapper.insertCollaborativeNote(collabNote);
//
//            // Add collaborators
//            if (request.getCollaborators() != null && !request.getCollaborators().isEmpty()) {
//                addCollaborators(collabNote.getId(), request.getCollaborators());
//            }
//
//            // Create edit history entry
//            createEditHistoryEntry(collabNote.getId(), user.getId(), "CREATE", "Note created");
//
//            CollaborativeNoteResponse response = mapToCollaborativeNoteResponse(collabNote, user.getUsername());
//            return new DataResponse<>(SUCCESS, "Collaborative note created successfully", HttpStatus.CREATED.value(), response);
//
//        } catch (Exception e) {
//            log.error("Error creating collaborative note for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    @Override
//    public DataResponse<List<CollaborativeNoteResponse>> getCollaborativeNotes(String slug) {
//        try {
//            String username = headerHolder.getUsername();
//            Book book = bookMapper.findBookBySlug(slug);
//
//            if (book == null) {
//                throw new DataNotFoundException();
//            }
//
//            List<CollaborativeNote> notes;
//            if (username != null && !username.isEmpty()) {
//                // Get notes user can access (authored by user or shared with user)
//                User user = userMapper.findUserByUsername(username);
//                notes = collaborativeNoteMapper.findAccessibleNotesByBook(book.getId(), user.getId());
//            } else {
//                // Get only public notes for guests
//                notes = collaborativeNoteMapper.findPublicNotesByBook(book.getId());
//            }
//
//            List<CollaborativeNoteResponse> responses = notes.stream()
//                    .map(note -> mapToCollaborativeNoteResponse(note, getAuthorName(note.getAuthorId())))
//                    .collect(Collectors.toList());
//
//            return new DataResponse<>(SUCCESS, "Collaborative notes retrieved successfully", HttpStatus.OK.value(), responses);
//
//        } catch (Exception e) {
//            log.error("Error getting collaborative notes for book: {}", slug, e);
//            throw e;
//        }
//    }
//
//    // ============ HELPER METHODS ============
//
//    private ReadingProgressResponse mapToReadingProgressResponse(ReadingProgress progress, Book book) {
//        ReadingProgressResponse response = new ReadingProgressResponse();
//        response.setId(progress.getId());
//        response.setBookId(progress.getBookId());
//        response.setBookTitle(book.getTitle());
//        response.setCurrentPage(progress.getCurrentPage());
//        response.setTotalPages(progress.getTotalPages());
//        response.setCurrentPosition(progress.getCurrentPosition());
//        response.setPercentageCompleted(progress.getPercentageCompleted());
//        response.setReadingTimeMinutes(progress.getReadingTimeMinutes());
//        response.setStatus(progress.getStatus());
//        response.setLastReadAt(progress.getLastReadAt());
//        response.setStartedAt(progress.getStartedAt());
//        return response;
//    }
//
//    private BookmarkResponse mapToBookmarkResponse(Bookmark bookmark) {
//        BookmarkResponse response = new BookmarkResponse();
//        response.setId(bookmark.getId());
//        response.setBookId(bookmark.getBookId());
//        response.setPage(bookmark.getPage());
//        response.setPosition(bookmark.getPosition());
//        response.setTitle(bookmark.getTitle());
//        response.setDescription(bookmark.getDescription());
//        response.setColor(bookmark.getColor());
//        response.setCreatedAt(bookmark.getCreatedAt());
//        return response;
//    }
//
//    private HighlightResponse mapToHighlightResponse(Highlight highlight) {
//        HighlightResponse response = new HighlightResponse();
//        response.setId(highlight.getId());
//        response.setBookId(highlight.getBookId());
//        response.setPage(highlight.getPage());
//        response.setStartPosition(highlight.getStartPosition());
//        response.setEndPosition(highlight.getEndPosition());
//        response.setHighlightedText(highlight.getHighlightedText());
//        response.setColor(highlight.getColor());
//        response.setNote(highlight.getNote());
//        response.setCreatedAt(highlight.getCreatedAt());
//        response.setUpdatedAt(highlight.getUpdatedAt());
//        return response;
//    }
//
//    private NoteResponse mapToNoteResponse(Note note) {
//        NoteResponse response = new NoteResponse();
//        response.setId(note.getId());
//        response.setBookId(note.getBookId());
//        response.setPage(note.getPage());
//        response.setPosition(note.getPosition());
//        response.setTitle(note.getTitle());
//        response.setContent(note.getContent());
//        response.setColor(note.getColor());
//        response.setIsPrivate(note.getIsPrivate());
//        response.setTags(getNoteTags(note.getId()));
//        response.setCommentCount(getNoteCommentCount(note.getId()));
//        response.setCreatedAt(note.getCreatedAt());
//        response.setUpdatedAt(note.getUpdatedAt());
//        return response;
//    }
//
//    private ReactionResponse mapToReactionResponse(Reaction reaction, String userName) {
//        ReactionResponse response = new ReactionResponse();
//        response.setId(reaction.getId());
//        response.setReactionType(reaction.getReactionType());
//        response.setPage(reaction.getPage());
//        response.setPosition(reaction.getPosition());
//        response.setUserName(userName);
//        response.setCreatedAt(reaction.getCreatedAt());
//
//        // Add reaction statistics
//        ReactionResponse.ReactionStats stats = getReactionStats(reaction.getBookId(), reaction.getPage(), reaction.getPosition());
//        response.setStats(stats);
//
//        return response;
//    }
//
//    private DiscussionResponse mapToDiscussionResponse(Discussion discussion) {
//        DiscussionResponse response = new DiscussionResponse();
//        response.setId(discussion.getId());
//        response.setTitle(discussion.getTitle());
//        response.setContent(discussion.getContent());
//        response.setAuthorName(getUserName(discussion.getUserId()));
//        response.setPage(discussion.getPage());
//        response.setPosition(discussion.getPosition());
//        response.setReplyCount(getDiscussionReplyCount(discussion.getId()));
//        response.setLikeCount(getDiscussionLikeCount(discussion.getId()));
//        response.setIsLikedByUser(isDiscussionLikedByCurrentUser(discussion.getId()));
//        response.setCreatedAt(discussion.getCreatedAt());
//        response.setReplies(getDiscussionReplies(discussion.getId()));
//        return response;
//    }
//
//    private VoiceNoteResponse mapToVoiceNoteResponse(VoiceNote voiceNote) {
//        VoiceNoteResponse response = new VoiceNoteResponse();
//        response.setId(voiceNote.getId());
//        response.setBookId(voiceNote.getBookId());
//        response.setPage(voiceNote.getPage());
//        response.setPosition(voiceNote.getPosition());
//        response.setAudioUrl(voiceNote.getAudioUrl());
//        response.setDuration(voiceNote.getDuration());
//        response.setTranscription(voiceNote.getTranscription());
//        response.setCreatedAt(voiceNote.getCreatedAt());
//        return response;
//    }
//
//    private CommentResponse mapToCommentResponse(NoteComment comment, String authorName) {
//        CommentResponse response = new CommentResponse();
//        response.setId(comment.getId());
//        response.setNoteId(comment.getNoteId());
//        response.setContent(comment.getContent());
//        response.setAuthorName(authorName);
//        response.setParentCommentId(comment.getParentCommentId());
//        response.setLikeCount(getCommentLikeCount(comment.getId()));
//        response.setIsLikedByUser(isCommentLikedByCurrentUser(comment.getId()));
//        response.setCreatedAt(comment.getCreatedAt());
//        response.setReplies(getCommentReplies(comment.getId()));
//        return response;
//    }
//
//    private CollaborativeNoteResponse mapToCollaborativeNoteResponse(CollaborativeNote note, String authorName) {
//        CollaborativeNoteResponse response = new CollaborativeNoteResponse();
//        response.setId(note.getId());
//        response.setBookId(note.getBookId());
//        response.setPage(note.getPage());
//        response.setPosition(note.getPosition());
//        response.setTitle(note.getTitle());
//        response.setContent(note.getContent());
//        response.setAuthorName(authorName);
//        response.setCollaborators(getCollaboratorNames(note.getId()));
//        response.setVisibility(note.getVisibility());
//        response.setEditCount(getEditCount(note.getId()));
//        response.setLastEditedAt(note.getLastEditedAt());
//        response.setLastEditedBy(getUserName(note.getLastEditedBy()));
//        response.setCreatedAt(note.getCreatedAt());
//        response.setEditHistory(getEditHistory(note.getId()));
//        return response;
//    }
//
//    // Additional helper methods for file operations, AI integration, etc.
//    private String readBookContent(String fileUrl) throws IOException {
//        // Implementation to read book content from file
//        // This would depend on your file format (EPUB, PDF, etc.)
//        return "Book content..."; // Placeholder
//    }
//
//    private List<SearchResultResponse.SearchResult> performTextSearch(String content, String query, int page,
//                                                                      int limit) {
//        // Implementation for full-text search
//        return new ArrayList<>(); // Placeholder
//    }
//
//    private String generateExportContent(List<Highlight> highlights, List<Note> notes, Book book, String
//            format) {
//        // Implementation to generate export content in specified format
//        return "Export content..."; // Placeholder
//    }
//
//    private String saveExportFile(String fileName, String content, String format) throws IOException {
//        // Implementation to save export file and return download URL
//        return "/downloads/" + fileName; // Placeholder
//    }
//
//    private Double estimateAudioDuration(String text) {
//        // Estimate audio duration based on text length (average reading speed)
//        return (double) text.length() / 10; // Rough estimate
//    }
//
//    private String generateTranscription(MultipartFile audioFile) {
//        // Implementation using speech-to-text service
//        return "Generated transcription..."; // Placeholder
//    }
//
//    private Double getAudioDuration(MultipartFile audioFile) {
//        // Get audio file duration
//        return 30.0; // Placeholder
//    }

// Add these missing methods to your BookServiceImpl class

    // Helper method to map ReadingProgress to Response
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

    // Helper method to map Bookmark to Response
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

    // Helper method to map Highlight to Response
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

    // Helper method to map Note to Response
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

    // Method to read book content from file
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

    // Method to perform text search in book content
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
                result.setContext(getSearchContext(lines, i, 2)); // 2 lines context
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

    // Helper method to get context around search result
    private String getSearchContext(String[] lines, int currentLine, int contextLines) {
        int start = Math.max(0, currentLine - contextLines);
        int end = Math.min(lines.length - 1, currentLine + contextLines);

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

    // Helper method to calculate relevance score
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

    // Helper method to highlight search term in text
    private String highlightSearchTerm(String text, String searchTerm) {
        if (text == null || searchTerm == null) {
            return text;
        }
        return text.replaceAll("(?i)" + Pattern.quote(searchTerm),
                "<mark>$0</mark>");
    }

    // Method to generate export content
    private String generateExportContent(List<Highlight> highlights, List<Note> notes, Book book, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generatePDFContent(highlights, notes, book);
            case "txt":
                return generateTextContent(highlights, notes, book);
            case "json":
                return generateJSONContent(highlights, notes, book);
            case "csv":
                return generateCSVContent(highlights, notes, book);
            default:
                return generateTextContent(highlights, notes, book);
        }
    }

    // Generate text format export
    private String generateTextContent(List<Highlight> highlights, List<Note> notes, Book book) {
        StringBuilder content = new StringBuilder();
        content.append("Book: ").append(book.getTitle()).append("\n");
        content.append("Export Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        content.append("=== HIGHLIGHTS ===\n");
        highlights.forEach(highlight -> {
            content.append("Page ").append(highlight.getPage()).append(": ");
            content.append(highlight.getHighlightedText()).append("\n");
            if (highlight.getNote() != null && !highlight.getNote().isEmpty()) {
                content.append("Note: ").append(highlight.getNote()).append("\n");
            }
            content.append("\n");
        });

        content.append("=== NOTES ===\n");
        notes.forEach(note -> {
            content.append("Page ").append(note.getPage()).append(" - ");
            content.append(note.getTitle()).append("\n");
            content.append(note.getContent()).append("\n\n");
        });

        return content.toString();
    }

    // Generate JSON format export
    private String generateJSONContent(List<Highlight> highlights, List<Note> notes, Book book) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            Map<String, Object> exportData = new HashMap<>();
            exportData.put("book", book);
            exportData.put("highlights", highlights);
            exportData.put("notes", notes);
            exportData.put("exportDate", LocalDateTime.now());

            return mapper.writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("Error generating JSON export", e);
            return generateTextContent(highlights, notes, book);
        }
    }

    // Generate CSV format export
    private String generateCSVContent(List<Highlight> highlights, List<Note> notes, Book book) {
        StringBuilder csv = new StringBuilder();
        csv.append("Type,Page,Title,Content,Color,Created At\n");

        highlights.forEach(highlight -> {
            csv.append("Highlight,")
                    .append(highlight.getPage()).append(",")
                    .append("\"").append(highlight.getHighlightedText().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(highlight.getNote() != null ? highlight.getNote().replace("\"", "\"\"") : "").append("\",")
                    .append(highlight.getColor()).append(",")
                    .append(highlight.getCreatedAt()).append("\n");
        });

        notes.forEach(note -> {
            csv.append("Note,")
                    .append(note.getPage()).append(",")
                    .append("\"").append(note.getTitle().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(note.getContent().replace("\"", "\"\"")).append("\",")
                    .append(note.getColor()).append(",")
                    .append(note.getCreatedAt()).append("\n");
        });

        return csv.toString();
    }

    // Generate PDF content (basic implementation)
    private String generatePDFContent(List<Highlight> highlights, List<Note> notes, Book book) {
        // For actual PDF generation, you would use libraries like iText or Apache PDFBox
        // This is a placeholder that returns HTML that could be converted to PDF
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>").append(book.getTitle()).append(" - Export</title></head><body>");
        html.append("<h1>").append(book.getTitle()).append("</h1>");

        html.append("<h2>Highlights</h2>");
        highlights.forEach(highlight -> {
            html.append("<div style='margin: 10px 0; padding: 10px; border-left: 3px solid ").append(highlight.getColor()).append(";'>");
            html.append("<strong>Page ").append(highlight.getPage()).append(":</strong> ");
            html.append(highlight.getHighlightedText());
            if (highlight.getNote() != null && !highlight.getNote().isEmpty()) {
                html.append("<br><em>").append(highlight.getNote()).append("</em>");
            }
            html.append("</div>");
        });

        html.append("<h2>Notes</h2>");
        notes.forEach(note -> {
            html.append("<div style='margin: 10px 0; padding: 10px; background-color: ").append(note.getColor()).append(";'>");
            html.append("<h3>").append(note.getTitle()).append("</h3>");
            html.append("<p>").append(note.getContent()).append("</p>");
            html.append("<small>Page ").append(note.getPage()).append("</small>");
            html.append("</div>");
        });

        html.append("</body></html>");
        return html.toString();
    }

    // Method to save export file
    private String saveExportFile(String fileName, String content, String format) throws IOException {
        try {
            // Create exports directory if it doesn't exist
            String exportDir = "exports/";
            Path exportPath = Paths.get(exportDir);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            // Full file path
            String filePath = exportDir + fileName;
            Path file = Paths.get(filePath);

            // Write content to file
            Files.writeString(file, content);

            // Return the download URL (adjust according to your application's URL structure)
            return "/api/downloads/" + fileName;

        } catch (Exception e) {
            log.error("Error saving export file: {}", fileName, e);
            throw new IOException("Failed to save export file", e);
        }
    }

    // Utility method to sanitize filename
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "untitled";
        }
        // Remove or replace invalid characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .trim();
    }
}