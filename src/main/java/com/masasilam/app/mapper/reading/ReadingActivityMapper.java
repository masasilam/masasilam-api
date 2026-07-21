package com.masasilam.app.mapper.reading;

import com.masasilam.app.model.entity.ReadingActivityLog;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingActivityMapper {
    @MapKey("chapter_number")
    List<Map<String, Object>> getUserChapterActivitySummary(@Param("userId") Long userId, @Param("bookId") Long bookId);
    @MapKey("id")
    Map<String, Object> getUserBookStatistics(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insert(ReadingActivityLog activity);
    Integer calculateAverageWpm(@Param("userId") Long userId, @Param("bookId") Long bookId);
}