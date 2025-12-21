package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.SearchHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {

    @Select("SELECT * FROM search_history " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<SearchHistory> getUserBookSearchHistory(@Param("userId") Long userId,
                                                 @Param("bookId") Long bookId,
                                                 @Param("limit") int limit);


    List<Map<String, Object>> searchInBook(
            @Param("bookId") Long bookId,
            @Param("query") String query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countSearchResults(
            @Param("bookId") Long bookId,
            @Param("query") String query
    );

    List<Map<String, Object>> searchInBookSimple(
            @Param("bookId") Long bookId,
            @Param("query") String query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countSearchResultsSimple(
            @Param("bookId") Long bookId,
            @Param("query") String query
    );

    void insertSearchHistory(SearchHistory history);
}
