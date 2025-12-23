package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingActivityLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingActivityMapper {

    @Insert("INSERT INTO reading_activity_log (" +
            "user_id, book_id, chapter_number, session_id, started_at, ended_at, " +
            "duration_seconds, start_position, end_position, scroll_depth_percentage, " +
            "words_read, reading_speed_wpm, is_skip, is_reread, interaction_count, " +
            "device_type, source, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{sessionId}, #{startedAt}, #{endedAt}, " +
            "#{durationSeconds}, #{startPosition}, #{endPosition}, #{scrollDepthPercentage}, " +
            "#{wordsRead}, #{readingSpeedWpm}, #{isSkip}, #{isReread}, #{interactionCount}, " +
            "#{deviceType}, #{source}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertActivity(ReadingActivityLog activity);

    @Update("UPDATE reading_activity_log SET " +
            "ended_at = #{endedAt}, duration_seconds = #{durationSeconds}, " +
            "end_position = #{endPosition}, scroll_depth_percentage = #{scrollDepthPercentage}, " +
            "words_read = #{wordsRead}, reading_speed_wpm = #{readingSpeedWpm}, " +
            "interaction_count = #{interactionCount} " +
            "WHERE session_id = #{sessionId} AND chapter_number = #{chapterNumber}")
    void updateActivity(ReadingActivityLog activity);

    @Select("SELECT * FROM reading_activity_log " +
            "WHERE session_id = #{sessionId} " +
            "AND chapter_number = #{chapterNumber} " +
            "AND ended_at IS NULL " +
            "ORDER BY started_at DESC LIMIT 1")
    ReadingActivityLog findBySessionAndChapter(@Param("sessionId") String sessionId, @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT * FROM reading_activity_log " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY started_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<ReadingActivityLog> findUserBookActivities(@Param("userId") Long userId,
                                                    @Param("bookId") Long bookId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    @Select("SELECT chapter_number, " +
            "COUNT(*) as times_read, " +
            "SUM(duration_seconds) as total_duration, " +
            "AVG(scroll_depth_percentage) as avg_scroll_depth, " +
            "MAX(started_at) as last_read_at " +
            "FROM reading_activity_log " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "GROUP BY chapter_number " +
            "ORDER BY chapter_number")
    List<Map<String, Object>> getUserChapterActivitySummary(@Param("userId") Long userId,
                                                            @Param("bookId") Long bookId);

    @Select("SELECT " +
            "COUNT(DISTINCT chapter_number) as chapters_read, " +
            "SUM(duration_seconds) as total_time, " +
            "AVG(reading_speed_wpm) as avg_speed " +
            "FROM reading_activity_log " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Map<String, Object> getUserBookStatistics(@Param("userId") Long userId,
                                              @Param("bookId") Long bookId);

    @Select("SELECT * FROM reading_activity WHERE user_id = #{userId} ORDER BY activity_time DESC")
    List<ReadingActivityLog> findAllByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM reading_activity WHERE user_id = #{userId} " +
            "AND activity_time >= #{since} ORDER BY activity_time DESC")
    List<ReadingActivityLog> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Insert("INSERT INTO reading_activity " +
            "(user_id, book_id, chapter_id, activity_type, activity_time, " +
            "duration_minutes, reading_speed_wpm, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterId}, #{activityType}, " +
            "#{activityTime}, #{durationMinutes}, #{readingSpeedWpm}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ReadingActivityLog activity);

    @Select("WITH session_stats AS (" +
            "  SELECT " +
            "    book_id, " +
            "    chapters_read, " +
            "    total_duration_seconds, " +
            "    start_chapter, " +
            "    end_chapter " +
            "  FROM reading_sessions " +
            "  WHERE user_id = #{userId} " +
            "    AND started_at >= #{start} " +
            "    AND started_at < #{end} " +
            "    AND chapters_read > 0" +
            "), word_counts AS (" +
            "  SELECT " +
            "    ss.book_id, " +
            "    SUM(bc.word_count) as session_words " +
            "  FROM session_stats ss " +
            "  LEFT JOIN book_chapters bc ON ss.book_id = bc.book_id " +
            "    AND bc.chapter_number >= ss.start_chapter " +
            "    AND bc.chapter_number <= COALESCE(ss.end_chapter, ss.start_chapter)" +
            "  WHERE bc.word_count IS NOT NULL " +
            "  GROUP BY ss.book_id, ss.start_chapter, ss.end_chapter" +
            ") " +
            "SELECT " +
            "  COUNT(DISTINCT ss.book_id) as books_read, " +
            "  COALESCE(SUM(ss.chapters_read), 0) as chapters_read, " +
            "  COALESCE(SUM(ss.total_duration_seconds), 0) as total_seconds, " +
            "  COALESCE(SUM(wc.session_words), 0) as total_words, " +
            "  CASE " +
            "    WHEN COALESCE(SUM(ss.total_duration_seconds), 0) > 0 " +
            "    THEN ROUND((COALESCE(SUM(wc.session_words), 0) * 60.0) / COALESCE(SUM(ss.total_duration_seconds), 1), 2) " +
            "    ELSE 200.0 " +
            "  END as avg_speed " +
            "FROM session_stats ss " +
            "LEFT JOIN word_counts wc ON ss.book_id = wc.book_id")
    Map<String, Object> getUserActivitySummary(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(total_duration_seconds / 60), 0) " +
            "FROM reading_sessions WHERE user_id = #{userId}")
    Integer getUserTotalReadingMinutes(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(total_duration_seconds) / 60, 0) " +
            "FROM reading_sessions " +
            "WHERE user_id = #{userId} AND started_at >= #{start} AND started_at < #{end}")
    Integer getTotalMinutesBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("SELECT EXISTS(" +
            "SELECT 1 FROM reading_sessions " +
            "WHERE user_id = #{userId} AND started_at >= #{since})")
    Boolean hasActivitySince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Select("SELECT * FROM reading_activity_log " +
            "WHERE session_id = #{sessionId} " +
            "AND chapter_number = #{chapterNumber} " +
            "AND ended_at IS NULL " +
            "LIMIT 1")
    ReadingActivityLog findActiveSession(@Param("sessionId") String sessionId,
                                         @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT COUNT(*) FROM reading_activity_log " +
            "WHERE user_id = #{userId} " +
            "AND book_id = #{bookId} " +
            "AND chapter_number = #{chapterNumber} " +
            "AND ended_at IS NOT NULL " +
            "AND id != #{excludeActivityId}")
    Integer countCompletedReads(@Param("userId") Long userId,
                                @Param("bookId") Long bookId,
                                @Param("chapterNumber") Integer chapterNumber,
                                @Param("excludeActivityId") Long excludeActivityId);

    @Select("SELECT COUNT(DISTINCT chapter_number) FROM reading_activity_log " +
            "WHERE session_id = #{sessionId} " +
            "AND ended_at IS NOT NULL")
    Integer countUniqueChaptersInSession(@Param("sessionId") String sessionId);

    @Select("SELECT COALESCE(SUM(interaction_count), 0) FROM reading_activity_log " +
            "WHERE session_id = #{sessionId} " +
            "AND ended_at IS NOT NULL")
    Integer sumInteractionsInSession(@Param("sessionId") String sessionId);

    @Select("SELECT AVG(reading_speed_wpm)::INTEGER FROM reading_activity_log " +
            "WHERE user_id = #{userId} " +
            "AND book_id = #{bookId} " +
            "AND reading_speed_wpm IS NOT NULL " +
            "AND reading_speed_wpm > 0")
    Integer calculateAverageWpm(@Param("userId") Long userId,
                                @Param("bookId") Long bookId);
}
