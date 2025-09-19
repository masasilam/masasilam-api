package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Highlight;
import com.naskah.demo.model.dto.response.HighlightTrendsResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HighlightMapper {

    @Insert("INSERT INTO highlights (user_id, book_id, page, start_position, end_position, highlighted_text, color, note, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{page}, #{startPosition}, #{endPosition}, #{highlightedText}, #{color}, #{note}, #{createdAt}, #{updatedAt})")
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
}
