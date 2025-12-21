package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Highlight;
import com.naskah.demo.model.dto.response.HighlightTrendsResponse;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HighlightMapper {

    @Insert("INSERT INTO highlights (user_id, book_id, chapter_number, chapter_title, chapter_slug, start_position, end_position, highlighted_text, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{chapterTitle}, #{chapterSlug}, #{startPosition}, #{endPosition}, #{highlightedText}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertHighlight(Highlight highlight);

    @Select("SELECT * FROM highlights WHERE user_id = #{userId} AND book_id = #{bookId} ORDER BY page ASC, start_position ASC")
    List<Highlight> findHighlightsByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT * FROM highlights WHERE id = #{id}")
    Highlight findHighlightById(@Param("id") Long id);

    @Update("UPDATE highlights SET highlighted_text = #{highlightedText}, color = #{color}, note = #{note}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateHighlight(Highlight highlight);

    @Delete("DELETE FROM highlights WHERE id = #{id}")
    void deleteHighlight(@Param("id") Long id);

    // For highlight trends
    @Select("SELECT highlighted_text as text, COUNT(*) as highlightCount, page, " +
            "(COUNT(*) * 1.0 / (SELECT COUNT(*) FROM highlights WHERE book_id = #{bookId})) as trendScore " +
            "FROM highlights WHERE book_id = #{bookId} " +
            "GROUP BY highlighted_text, page " +
            "ORDER BY highlightCount DESC " +
            "LIMIT 20")
    List<HighlightTrendsResponse.TrendingHighlight> getTrendingHighlights(@Param("bookId") Long bookId);

    @Select("SELECT page, COUNT(*) as highlightCount, " +
            "(SELECT highlighted_text FROM highlights h2 WHERE h2.book_id = #{bookId} AND h2.page = h1.page " +
            "GROUP BY highlighted_text ORDER BY COUNT(*) DESC LIMIT 1) as mostHighlightedText " +
            "FROM highlights h1 WHERE book_id = #{bookId} " +
            "GROUP BY page " +
            "ORDER BY highlightCount DESC " +
            "LIMIT 10")
    List<HighlightTrendsResponse.PopularPage> getPopularPages(@Param("bookId") Long bookId);

    @Select("SELECT COUNT(*) FROM highlights WHERE book_id = #{bookId}")
    Integer countHighlightsByBook(@Param("bookId") Long bookId);

    List<Highlight> findByUserBookAndPage(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("page") Integer page
    );

    @Select("SELECT * FROM highlights WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<Highlight> findByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM highlights WHERE user_id = #{userId} " +
            "AND created_at >= #{since} " +
            "ORDER BY created_at DESC")
    List<Highlight> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Select("SELECT * FROM highlights WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY chapter_number, created_at DESC")
    List<Highlight> findByUserAndBook(@Param("userId") Long userId,
                                      @Param("bookId") Long bookId);

    @Select("SELECT COUNT(*) FROM highlights WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM highlights " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByBookAndUser(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM highlights " +
            "WHERE user_id = #{userId} AND is_reviewed = false")
    Integer countUnreadByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM highlights WHERE id = #{id}")
    Highlight findById(@Param("id") Long id);

    @Insert("INSERT INTO highlights " +
            "(user_id, book_id, chapter_number, highlighted_text, color, " +
            "start_offset, end_offset, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{highlightedText}, " +
            "#{color}, #{startOffset}, #{endOffset}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Highlight highlight);

    @Update("UPDATE highlights SET " +
            "color = #{color}, " +
            "is_reviewed = #{isReviewed}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(Highlight highlight);

    @Delete("DELETE FROM highlights WHERE id = #{id}")
    void delete(@Param("id") Long id);

    @Select("SELECT * FROM highlights " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Highlight> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);


    @Select("SELECT COUNT(*) FROM highlights " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    @Select("SELECT * FROM highlights WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND chapter_number = #{chapterNumber} ORDER BY created_at DESC")
    List<Highlight> findByUserBookAndChapter(@Param("userId") Long userId,
                                             @Param("bookId") Long bookId,
                                             @Param("chapterNumber") Integer chapterNumber);
}
