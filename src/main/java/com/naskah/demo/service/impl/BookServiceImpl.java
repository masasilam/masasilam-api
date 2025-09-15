package com.naskah.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.exception.custom.DataAlreadyExistsException;
import com.naskah.demo.exception.custom.ForbiddenException;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.AuthorRequest;
import com.naskah.demo.model.dto.request.BookRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.BookService;
import com.naskah.demo.util.file.BookMetadata;
import com.naskah.demo.util.file.FileStorageResult;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final String SUCCESS = "Success";

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
//
//    @Override
//    public Resource downloadEbook(String id) throws MalformedURLException {
//        try {
//            Book ebook = bookMapper.getDetailEbook(id);
//            if (ebook == null) {
//                throw new NotFoundException();
//            }
//
//            Path filePath = Paths.get(ebook.getFilePath());
//            Resource resource = new UrlResource(filePath.toUri());
//            if (resource.exists() || resource.isReadable()) {
//                return resource;
//            } else {
//                throw new NotFoundException();
//            }
//        } catch (Exception e) {
//            log.error("Error when download ebook", e);
//            throw e;
//        }
//    }
//
//    @Override
//    public Resource readEbook(String id) throws MalformedURLException {
//        return downloadEbook(id);
//    }
}
