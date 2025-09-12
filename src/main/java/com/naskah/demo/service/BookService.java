package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.BookRequest;
import com.naskah.demo.model.dto.response.BookResponse;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.DatatableResponse;
import com.naskah.demo.model.dto.response.ReadingResponse;

import java.util.List;

public interface BookService {
    DataResponse<BookResponse> createBook(BookRequest request);

    DataResponse<BookResponse> getBookDetailBySlug(String slug);

    DataResponse<ReadingResponse> startReading(String slug);

    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder,
                                                      String searchTitle, Long seriesId, Long genreId, Long subGenreId);
//    DataResponse<Book> update(String id, Book book, MultipartFile file) throws IOException;
//    DefaultResponse delete(String id) throws IOException;
//    Resource downloadEbook(String id) throws MalformedURLException;
//    Resource readEbook(String id) throws MalformedURLException;
}
