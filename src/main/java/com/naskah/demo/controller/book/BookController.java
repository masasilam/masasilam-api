package com.naskah.demo.controller.book;

import com.naskah.demo.model.dto.BookSearchCriteria;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.service.book.BookChapterService;
import com.naskah.demo.service.book.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * BookController - Handle basic book CRUD operations + metadata
 * <p>
 * CRUD Endpoints:
 * - POST   /api/books                 - Create book
 * - GET    /api/books/{slug}          - Get book detail
 * - GET    /api/books                 - Get books (paginated, filtered)
 * - PUT    /api/books                 - Update book
 * - DELETE /api/books/{id}            - Delete book
 * <p>
 * Metadata Endpoints:
 * - GET    /api/books/genres          - Get all genres
 * - GET    /api/books/authors         - Get all authors
 * - GET    /api/books/contributors    - Get all contributors
 * <p>
 * Other Endpoints:
 * - GET    /api/books/{slug}/download        - Download book file
 * - GET    /api/books/{slug}/my-annotations  - Get user's annotations
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookChapterService chapterService;

    // ============================================
    // BOOK CRUD OPERATIONS
    // ============================================

    /**
     * Create new book with EPUB file
     * Auto-extracts metadata, cover, chapters
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BookResponse>> createBook(@Valid @ModelAttribute BookRequest request) {
        DataResponse<BookResponse> response = bookService.createBook(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get detailed book information by slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<BookResponse>> getBookDetail(@PathVariable String slug) {
        DataResponse<BookResponse> response = bookService.getBookDetailBySlug(slug);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<BookResponse>> getBooksPaginated(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                             @RequestParam(defaultValue = "12") @Min(1) int limit,
                                                                             @RequestParam(defaultValue = "updateAt") String sortField,
                                                                             @RequestParam(defaultValue = "DESC") String sortOrder,
                                                                             @RequestParam(required = false) String searchTitle,
                                                                             @RequestParam(required = false) String searchInBook,
                                                                             @RequestParam(required = false) String authorName,
                                                                             @RequestParam(required = false) String contributor,
                                                                             @RequestParam(required = false) String genre,
                                                                             @RequestParam(required = false) Integer minChapters,
                                                                             @RequestParam(required = false) Integer maxChapters,
                                                                             @RequestParam(required = false) Long minFileSize,
                                                                             @RequestParam(required = false) Long maxFileSize,
                                                                             @RequestParam(required = false) Integer publicationYearFrom,
                                                                             @RequestParam(required = false) Integer publicationYearTo,
                                                                             @RequestParam(required = false) String difficultyLevel,
                                                                             @RequestParam(required = false) String fileFormat,
                                                                             @RequestParam(required = false) Boolean isFeatured,
                                                                             @RequestParam(required = false) Integer languageId,
                                                                             @RequestParam(required = false) Double minRating,
                                                                             @RequestParam(required = false) Integer minViewCount,
                                                                             @RequestParam(required = false) Integer minReadCount) {
        BookSearchCriteria criteria = BookSearchCriteria.builder()
                .searchTitle(searchTitle)
                .searchInBook(searchInBook)
                .authorName(authorName)
                .contributor(contributor)
                .genre(genre)
                .minPages(minChapters)
                .maxPages(maxChapters)
                .minFileSize(minFileSize)
                .maxFileSize(maxFileSize)
                .publicationYearFrom(publicationYearFrom)
                .publicationYearTo(publicationYearTo)
                .difficultyLevel(difficultyLevel)
                .fileFormat(fileFormat)
                .isFeatured(isFeatured)
                .languageId(languageId)
                .minRating(minRating)
                .minViewCount(minViewCount)
                .minReadCount(minReadCount)
                .build();

        DatatableResponse<BookResponse> response = bookService.getPaginatedBooks(page, limit, sortField, sortOrder, criteria);

        return ResponseEntity.ok(response);
    }

    /**
     * Update existing book
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Book>> updateBook(@RequestParam Long id, @RequestPart("ebook") @Valid Book book,
                                                         @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        DataResponse<Book> response = bookService.update(id, book, file);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete book and all associated data
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DefaultResponse> deleteBook(@PathVariable Long id) throws IOException {
        DefaultResponse response = bookService.delete(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Download book file (EPUB/PDF)
     * Rate-limited for guest users
     */
    @GetMapping("/{slug}/download")
    public ResponseEntity<byte[]> downloadBook(@PathVariable String slug) {
        return bookService.downloadBookAsBytes(slug);
    }

    // ============================================
    // METADATA OPERATIONS
    // ============================================

    /**
     * Get all genres with book count
     * Returns list of genres sorted by name
     */
    @GetMapping("/genres")
    public ResponseEntity<DataResponse<List<GenreResponse>>> getAllGenres(@RequestParam(defaultValue = "false") boolean includeBookCount) {
        DataResponse<List<GenreResponse>> response = bookService.getAllGenres(includeBookCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all authors with book count and details
     * Supports pagination and search
     *
     * @param page - Page number (default: 1)
     * @param limit - Items per page (default: 20)
     * @param search - Search by author name (optional)
     * @param sortBy - Sort by: name, bookCount, createdAt (default: name)
     */
    @GetMapping("/authors")
    public ResponseEntity<DatatableResponse<AuthorResponse>> getAllAuthors(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                           @RequestParam(defaultValue = "20") @Min(1) int limit,
                                                                           @RequestParam(required = false) String search,
                                                                           @RequestParam(defaultValue = "name") String sortBy) {
        DatatableResponse<AuthorResponse> response = bookService.getAllAuthors(page, limit, search, sortBy);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all contributors with role information
     * Supports pagination and filtering by role
     *
     * @param page - Page number (default: 1)
     * @param limit - Items per page (default: 20)
     * @param role - Filter by role: TRANSLATOR, ILLUSTRATOR, EDITOR, etc. (optional)
     * @param search - Search by contributor name (optional)
     */
    @GetMapping("/contributors")
    public ResponseEntity<DatatableResponse<ContributorResponse>> getAllContributors(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                     @RequestParam(defaultValue = "20") @Min(1) int limit,
                                                                                     @RequestParam(required = false) String role,
                                                                                     @RequestParam(required = false) String search) {
        DatatableResponse<ContributorResponse> response = bookService.getAllContributors(page, limit, role, search);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // USER ANNOTATIONS
    // ============================================

    /**
     * Get all user's annotations across ALL chapters in this book
     * Returns: bookmarks, highlights, notes from entire book
     */
    @GetMapping("/{slug}/my-annotations")
    public ResponseEntity<DataResponse<ChapterAnnotationsResponse>> getMyBookAnnotations(@PathVariable String slug) {
        DataResponse<ChapterAnnotationsResponse> response = chapterService.getMyChapterAnnotations(slug);

        return ResponseEntity.ok(response);
    }
}