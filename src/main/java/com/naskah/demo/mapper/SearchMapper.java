package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.SearchHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {

    @Select("SELECT bc.id as chapter_id, bc.chapter_number, bc.title as chapter_title, " +
            "bc.slug as chapter_slug, bc.chapter_level, " +
            "ts_rank(bc.search_vector, query) as relevance_score, " +
            "ts_headline('indonesian', bc.content, query, " +
            "'MaxWords=50, MinWords=25, ShortWord=3, HighlightAll=false, MaxFragments=3') as snippet " +
            "FROM book_chapters bc, " +
            "to_tsquery('indonesian', #{query}) query " +
            "WHERE bc.book_id = #{bookId} " +
            "AND bc.search_vector @@ query " +
            "ORDER BY relevance_score DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> searchInBook(@Param("bookId") Long bookId,
                                           @Param("query") String query,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM book_chapters bc, " +
            "to_tsquery('indonesian', #{query}) query " +
            "WHERE bc.book_id = #{bookId} AND bc.search_vector @@ query")
    int countSearchResults(@Param("bookId") Long bookId,
                           @Param("query") String query);

    @Insert("INSERT INTO search_history (user_id, book_id, query, results_count, search_type, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{query}, #{resultsCount}, #{searchType}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSearchHistory(SearchHistory search);

    @Select("SELECT * FROM search_history " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<SearchHistory> getUserBookSearchHistory(@Param("userId") Long userId,
                                                 @Param("bookId") Long bookId,
                                                 @Param("limit") int limit);
}
