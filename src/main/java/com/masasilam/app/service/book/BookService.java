package com.masasilam.app.service.book;

import com.masasilam.app.model.dto.BookSearchCriteria;
import com.masasilam.app.model.dto.CompleteEpubMetadata;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.Book;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface BookService {
    DataResponse<BookResponse> createBook(BookRequest request);
    DataResponse<BookResponse> getBookDetailBySlug(String slug, HttpServletRequest request) throws NoSuchAlgorithmException;
    ResponseEntity<?> getDownloadUrl(String slug, HttpServletRequest request);
    DataResponse<Book> update(Long id, Book book, MultipartFile file) throws IOException;
    DefaultResponse delete(Long id) throws IOException;
    DataResponse<List<GenreResponse>> getAllGenres(boolean includeBookCount);
    DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy);
    DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search);
    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder, BookSearchCriteria criteria);
    List<Book> getAllBooksForSitemap();
    List<String> getChapterPaths(String slug);
    DataResponse<BookResponse> updateExistingBook(Book existingBook, MultipartFile newFile, CompleteEpubMetadata epubMeta) throws IOException;
    DatatableResponse<BookResponse> getBooksBySeries(String seriesSlug, int page, int limit);
}