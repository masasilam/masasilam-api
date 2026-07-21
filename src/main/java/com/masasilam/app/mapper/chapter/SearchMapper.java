package com.masasilam.app.mapper.chapter;

import com.masasilam.app.model.entity.SearchHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    List<SearchHistory> getUserBookSearchHistory(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("limit") int limit);
    @MapKey("id")
    List<Map<String, Object>> searchInBook(@Param("bookId") Long bookId, @Param("query") String query, @Param("offset") int offset, @Param("limit") int limit);
    int countSearchResults(@Param("bookId") Long bookId, @Param("query") String query);
    @MapKey("id")
    List<Map<String, Object>> searchInBookSimple(@Param("bookId") Long bookId, @Param("query") String query, @Param("offset") int offset, @Param("limit") int limit);
    int countSearchResultsSimple(@Param("bookId") Long bookId, @Param("query") String query);
    void insertSearchHistory(SearchHistory history);
}