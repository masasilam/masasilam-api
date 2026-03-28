package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.BookSearchCriteria;
import com.naskah.demo.model.dto.response.BookRecommendationResponse;
import com.naskah.demo.model.dto.response.BookResponse;
import com.naskah.demo.model.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper {

    // ── BOOK CREATION ─────────────────────────────────────────
    int countBySlug(@Param("slug") String slug);
    void insertBook(Book book);
    void insertBookGenre(@Param("bookId") Long bookId, @Param("genreId") Long genreId);
    void insertBookTag(@Param("bookId") Long bookId, @Param("tagId") Long tagId);
    void insertBookAuthor(@Param("bookId") Long bookId, @Param("authorId") Long authorId);
    void insertBookContributor(@Param("bookId") Long bookId,
                               @Param("contributorId") Long contributorId,
                               @Param("role") String role);

    // ── BOOK RETRIEVAL ────────────────────────────────────────
    BookResponse getBookDetailBySlug(@Param("slug") String slug);
    Book findBookBySlug(@Param("slug") String slug);
    Book findBySlug(@Param("slug") String slug);
    Book findById(@Param("id") Long id);
    Book getDetailEbook(@Param("id") Long id);
    Long getBookIdBySlug(@Param("slug") String slug);
    List<Book> findAllBooksForSitemap();
    List<String> getChapterPathsForSitemap(@Param("bookSlug") String bookSlug);

    // ── BOOK LIST ─────────────────────────────────────────────
    List<BookResponse> getBookListWithFilters(
            @Param("searchTitle") String searchTitle,
            @Param("seriesId") Long seriesId,
            @Param("genreId") Long genreId,
            @Param("subGenreId") Long subGenreId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType);

    List<BookResponse> getBookListWithAdvancedFilters(
            @Param("criteria") BookSearchCriteria criteria,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType);

    int countBooksWithAdvancedFilters(@Param("criteria") BookSearchCriteria criteria);

    // ── BOOK UPDATE / DELETE ──────────────────────────────────
    void updateBook(Book book);
    void deleteEbook(@Param("id") Long id);
    void deleteBook(@Param("id") Long id);

    // ── RELATIONSHIPS ─────────────────────────────────────────
    void deleteBookGenres(@Param("bookId") Long bookId);
    void deleteBookContributors(@Param("bookId") Long bookId);
    void deleteBookAuthors(@Param("bookId") Long bookId);

    // ── AUTHORS ───────────────────────────────────────────────
    List<Author> findAuthorsByBookId(@Param("bookId") Long bookId);
    List<Author> getBookAuthors(@Param("bookId") Long bookId);
    List<String> findAuthorNamesByBookId(@Param("bookId") Long bookId);

    // ── GENRES ────────────────────────────────────────────────
    List<Genre> getBookGenres(@Param("bookId") Long bookId);
    List<Genre> findGenresByBookId(@Param("bookId") Long bookId);
    List<String> findGenreNamesByBookId(@Param("bookId") Long bookId);

    // ── CONTRIBUTORS ──────────────────────────────────────────
    List<Contributor> findContributorsByBookId(@Param("bookId") Long bookId);

    // ── COPYRIGHT ─────────────────────────────────────────────
    String findRightsByCopyrightStatusId(@Param("copyrightStatusId") Integer copyrightStatusId);

    // ── COUNTERS ──────────────────────────────────────────────
    void incrementReadCount(@Param("bookId") Long bookId);
    void incrementDownloadCount(@Param("id") Long id);
    void incrementViewCountBySlug(@Param("slug") String slug);
    int countUserReadSessions(@Param("bookId") Long bookId, @Param("userId") Long userId);

    // ── VIEWS / ACTIONS ───────────────────────────────────────
    boolean hasActionByHash(@Param("viewerHash") String viewerHash,
                            @Param("actionType") String actionType);
    void insertAction(BookView bookView);
    int getUniqueViewCount(@Param("slug") String slug);
    int getUniqueDownloadCount(@Param("slug") String slug);

    // ── RECOMMENDATIONS ───────────────────────────────────────
    List<BookRecommendationResponse> getRecommendations(
            @Param("userId") Long userId,
            @Param("genres") List<String> genres,
            @Param("limit") int limit);

    List<Book> findBooksByGenreAndUser(
            @Param("userId") Long userId,
            @Param("genreName") String genreName,
            @Param("since") LocalDateTime since);

    String findLanguageNameByBookId(@Param("bookId") Long bookId);
    String findCopyrightStatusCodeByBookId(@Param("bookId") Long bookId);
}