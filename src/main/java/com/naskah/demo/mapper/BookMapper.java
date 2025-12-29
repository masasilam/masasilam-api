package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.BookSearchCriteria;
import com.naskah.demo.model.dto.response.BookRecommendationResponse;
import com.naskah.demo.model.dto.response.BookResponse;
import com.naskah.demo.model.entity.Author;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookView;
import com.naskah.demo.model.entity.Genre;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper {
    // Book creation methods
    int countBySlug(@Param("slug") String slug);
    void insertBook(Book book);
    void insertBookGenre(@Param("bookId") Long bookId, @Param("genreId") Long genreId);
    void insertBookTag(@Param("bookId") Long bookId, @Param("tagId") Long tagId);
    void insertBookAuthor(@Param("bookId") Long bookId, @Param("authorId") Long authorId);
    void insertBookContributor(@Param("bookId") Long bookId, @Param("contributorId") Long contributorId, @Param("role") String role);

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
    void updateBook(Book book);
    void deleteEbook(@Param("id") Long id);  // Changed from String to Long

    void incrementDownloadCount(@Param("id") Long id);

    Book findById(@Param("id")Long bookId);

    List<BookResponse> getBookListWithAdvancedFilters(
            @Param("criteria") BookSearchCriteria criteria,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType);

    int countBooksWithAdvancedFilters(@Param("criteria") BookSearchCriteria criteria);

    @Select("SELECT b.id, b.title, b.slug, b.cover_image_url, " +
            "a.name as author_name, g.name as genre, " +
            "COALESCE(AVG(br.rating), 0) as average_rating, " +
            "COUNT(DISTINCT rs.user_id) as total_readers " +
            "FROM books b " +
            "LEFT JOIN book_authors ba ON b.id = ba.book_id " +
            "LEFT JOIN authors a ON ba.author_id = a.id " +
            "LEFT JOIN book_genres bg ON b.id = bg.book_id " +
            "LEFT JOIN genres g ON bg.genre_id = g.id " +
            "LEFT JOIN book_ratings br ON b.id = br.book_id " +
            "LEFT JOIN reading_sessions rs ON b.id = rs.book_id " +
            "WHERE g.name IN " +
            "<foreach item='genre' collection='genres' open='(' separator=',' close=')'>" +
            "  #{genre}" +
            "</foreach> " +
            "AND b.id NOT IN (" +
            "  SELECT DISTINCT book_id FROM reading_sessions WHERE user_id = #{userId}" +
            ") " +
            "GROUP BY b.id, b.title, b.slug, b.cover_image_url, a.name, g.name " +
            "ORDER BY average_rating DESC, total_readers DESC " +
            "LIMIT #{limit}")
    List<BookRecommendationResponse> getRecommendations(
            @Param("userId") Long userId,
            @Param("genres") List<String> genres,
            @Param("limit") int limit);

    @Select("SELECT a.* FROM authors a " +
            "JOIN book_authors ba ON a.id = ba.author_id " +
            "WHERE ba.book_id = #{bookId}")
    List<Author> getBookAuthors(@Param("bookId") Long bookId);

    @Select("SELECT g.* FROM genres g " +
            "JOIN book_genres bg ON g.id = bg.genre_id " +
            "WHERE bg.book_id = #{bookId}")
    List<Genre> getBookGenres(@Param("bookId") Long bookId);

    // Tambahkan di BookMapper interface
    @Select("SELECT DISTINCT b.* FROM books b " +
            "JOIN book_genres bg ON b.id = bg.book_id " +
            "JOIN genres g ON bg.genre_id = g.id " +
            "JOIN reading_sessions rs ON b.id = rs.book_id " +
            "WHERE rs.user_id = #{userId} " +
            "AND g.name = #{genreName} " +
            "AND rs.started_at >= #{since} " +
            "ORDER BY rs.started_at DESC")
    List<Book> findBooksByGenreAndUser(
            @Param("userId") Long userId,
            @Param("genreName") String genreName,
            @Param("since") LocalDateTime since);

    void incrementViewCountBySlug(@Param("slug") String slug);

    int countUserReadSessions(@Param("bookId") Long bookId, @Param("userId") Long userId);

    /**
     * Find all authors associated with a book
     */
    @Select("SELECT a.* FROM authors a " +
            "INNER JOIN book_authors ba ON a.id = ba.author_id " +
            "WHERE ba.book_id = #{bookId}")
    List<Author> findAuthorsByBookId(@Param("bookId") Long bookId);

    /**
     * Delete all genre relationships for a book
     */
    @Delete("DELETE FROM book_genres WHERE book_id = #{bookId}")
    void deleteBookGenres(@Param("bookId") Long bookId);

    /**
     * Delete all contributor relationships for a book
     */
    @Delete("DELETE FROM book_contributors WHERE book_id = #{bookId}")
    void deleteBookContributors(@Param("bookId") Long bookId);

    /**
     * Optional: Delete author relationships (if you want to re-create them)
     * Note: Usually we keep author relationships and just add new ones if needed
     */
    @Delete("DELETE FROM book_authors WHERE book_id = #{bookId}")
    void deleteBookAuthors(@Param("bookId") Long bookId);

    @Select("SELECT * FROM books WHERE slug = #{slug}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "slug", column = "slug"),
            @Result(property = "subtitle", column = "subtitle"),
            @Result(property = "seriesId", column = "series_id"),
            @Result(property = "seriesOrder", column = "series_order"),
            @Result(property = "edition", column = "edition"),
            @Result(property = "publicationYear", column = "publication_year"),
            @Result(property = "publisher", column = "publisher"),
            @Result(property = "languageId", column = "language_id"),
            @Result(property = "description", column = "description"),
            @Result(property = "fileUrl", column = "file_url"),
            @Result(property = "coverImageUrl", column = "cover_image_url"),
            @Result(property = "source", column = "source"),
            @Result(property = "fileFormat", column = "file_format"),
            @Result(property = "fileSize", column = "file_size"),
            @Result(property = "totalPages", column = "total_pages"),
            @Result(property = "totalWord", column = "total_word"),
            @Result(property = "estimatedReadTime", column = "estimated_read_time"),
            @Result(property = "copyrightStatusId", column = "copyright_status_id"),
            @Result(property = "viewCount", column = "view_count"),
            @Result(property = "readCount", column = "read_count"),
            @Result(property = "downloadCount", column = "download_count"),
            @Result(property = "isActive", column = "is_active"),
            @Result(property = "isFeatured", column = "is_featured"),
            @Result(property = "publishedAt", column = "published_at"),
            @Result(property = "category", column = "category"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Book findBySlug(@Param("slug") String slug);

    @Select("SELECT id, title, slug, cover_image_url, " +
            "updated_at, created_at, is_active " +
            "FROM books " +
            "WHERE is_active = true " +
            "ORDER BY updated_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "slug", column = "slug"),
            @Result(property = "coverImageUrl", column = "cover_image_url"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "isActive", column = "is_active")
    })
    List<Book> findAllBooksForSitemap();

    @Select("WITH RECURSIVE chapter_paths AS ( " +
            "    SELECT " +
            "        c.id, " +
            "        c.book_id, " +
            "        c.slug, " +
            "        c.parent_chapter_id, " +
            "        c.slug::TEXT as full_path " +
            "    FROM book_chapters c " +
            "    INNER JOIN books b ON c.book_id = b.id " +
            "    WHERE b.slug = #{bookSlug} AND c.parent_chapter_id IS NULL " +
            "    UNION ALL " +
            "    SELECT " +
            "        c.id, " +
            "        c.book_id, " +
            "        c.slug, " +
            "        c.parent_chapter_id, " +
            "        (cp.full_path || '/' || c.slug)::TEXT " +
            "    FROM book_chapters c " +
            "    INNER JOIN chapter_paths cp ON c.parent_chapter_id = cp.id " +
            "    WHERE c.book_id = cp.book_id " +
            ") " +
            "SELECT full_path FROM chapter_paths " +
            "ORDER BY id ASC")
    List<String> getChapterPathsForSitemap(@Param("bookSlug") String bookSlug);

    @Select("SELECT COUNT(*) > 0 FROM book_views " +
            "WHERE viewer_hash = #{viewerHash} AND action_type = #{actionType}")
    boolean hasActionByHash(@Param("viewerHash") String viewerHash,
                            @Param("actionType") String actionType);

    // Insert action
    @Insert("INSERT INTO book_views (book_id, slug, user_id, ip_address, user_agent, viewer_hash, action_type, viewed_at) " +
            "VALUES (#{bookId}, #{slug}, #{userId}, #{ipAddress}, #{userAgent}, #{viewerHash}, #{actionType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertAction(BookView bookView);

    @Select("SELECT id FROM books WHERE slug = #{slug}")
    Long getBookIdBySlug(@Param("slug") String slug);

    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE viewer_hash END) " +
            "FROM book_views WHERE slug = #{slug} AND action_type = 'view'")
    int getUniqueViewCount(@Param("slug") String slug);

    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE viewer_hash END) " +
            "FROM book_views WHERE slug = #{slug} AND action_type = 'download'")
    int getUniqueDownloadCount(@Param("slug") String slug);
}