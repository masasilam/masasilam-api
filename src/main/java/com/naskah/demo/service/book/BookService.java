package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.BookSearchCriteria;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.Genre;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BookService {
    DataResponse<BookResponse> createBook(BookRequest request);
    DataResponse<BookResponse> getBookDetailBySlug(String slug, HttpServletRequest request);
    ResponseEntity<byte[]> downloadBookAsBytes(String slug, HttpServletRequest request);
    DataResponse<Book> update(Long id, Book book, MultipartFile file) throws IOException;
    DefaultResponse delete(Long id) throws IOException;
    DataResponse<List<GenreResponse>> getAllGenres(boolean includeBookCount);
    DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy);
    DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search);
    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder, BookSearchCriteria criteria);
    List<Book> getAllBooksForSitemap();
    List<String> getChapterPaths(String slug);
}