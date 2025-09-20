package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.BookResponse;
import com.naskah.demo.model.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper {
    // Book creation methods
    int countByTitleAndPublicationYear(@Param("title") String title, @Param("publicationYear") Integer publicationYear);
    void insertBook(Book book);
    void insertBookGenre(@Param("bookId") Long bookId, @Param("genreId") Long genreId);
    void insertBookTag(@Param("bookId") Long bookId, @Param("tagId") Long tagId);
    void insertBookAuthor(@Param("bookId") Long bookId, @Param("authorId") Long authorId);

    // Book retrieval methods
    BookResponse getBookDetailBySlug(@Param("slug") String slug);
    Book findBookBySlug(@Param("slug") String slug);

    // Fixed method name (was incrementRaeadCount - typo)
    void incrementReadCount(@Param("bookId") Long bookId);

    // Paginated book list
    List<BookResponse> getBookListWithFilters(
            @Param("searchTitle") String searchTitle,
            @Param("seriesId") Long seriesId,
            @Param("genreId") Long genreId,
            @Param("subGenreId") Long subGenreId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    // Book update/delete methods - fixed method names and parameter types
    Book getDetailEbook(@Param("id") Long id);  // Changed from String to Long
    void updateEbook(Book book);
    void deleteEbook(@Param("id") Long id);  // Changed from String to Long
}