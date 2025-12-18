package com.naskah.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsMapper {

    Map<String, Object> getBookOverviewMetrics(@Param("bookId") Long bookId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    List<Map<String, Object>> getReadersByDevice(@Param("bookId") Long bookId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    List<Map<String, Object>> getReadingHourDistribution(@Param("bookId") Long bookId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    Map<String, Object> getReadingPatterns(@Param("bookId") Long bookId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    Map<String, Object> getEngagementRates(@Param("bookId") Long bookId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    Map<String, Object> getAnnotationCounts(@Param("bookId") Long bookId);

    List<Map<String, Object>> getTopEngagedChapters(@Param("bookId") Long bookId,
                                                    @Param("limit") int limit);

    List<Map<String, Object>> getMostHighlightedPassages(@Param("bookId") Long bookId,
                                                         @Param("limit") int limit);

    List<Map<String, Object>> getDropOffPoints(@Param("bookId") Long bookId);

    List<Map<String, Object>> getMostSkippedChapters(@Param("bookId") Long bookId);

    Map<String, Object> getChapterStats(@Param("bookId") Long bookId,
                                        @Param("chapterNumber") Integer chapterNumber);

    List<Map<String, Object>> getTopHighlightsForChapter(@Param("bookId") Long bookId,
                                                         @Param("chapterNumber") Integer chapterNumber);

    void updateHighlightHeatmap(@Param("bookId") Long bookId,
                                @Param("chapterNumber") Integer chapterNumber,
                                @Param("count") Integer count);

    void updateNoteHeatmap(@Param("bookId") Long bookId,
                           @Param("chapterNumber") Integer chapterNumber,
                           @Param("count") Integer count);

    void updateReadingHeatmap(@Param("bookId") Long bookId,
                              @Param("chapterNumber") Integer chapterNumber,
                              @Param("count") Integer count);
}