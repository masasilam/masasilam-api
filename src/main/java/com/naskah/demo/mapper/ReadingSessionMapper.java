package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadingSessionMapper {

    @Insert("INSERT INTO reading_sessions (" +
            "user_id, book_id, session_id, started_at, device_type, device_id, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{sessionId}, #{startedAt}, #{deviceType}, #{deviceId}, " +
            "#{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSession(ReadingSession session);

    @Update("UPDATE reading_sessions SET " +
            "ended_at = #{endedAt}, total_duration_seconds = #{totalDurationSeconds}, " +
            "chapters_read = #{chaptersRead}, start_chapter = #{startChapter}, " +
            "end_chapter = #{endChapter}, completion_delta = #{completionDelta}, " +
            "total_interactions = #{totalInteractions}, updated_at = #{updatedAt} " +
            "WHERE session_id = #{sessionId}")
    void updateSession(ReadingSession session);

    @Select("SELECT * FROM reading_sessions WHERE session_id = #{sessionId}")
    ReadingSession findBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM reading_sessions " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY started_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<ReadingSession> findUserBookSessions(@Param("userId") Long userId,
                                              @Param("bookId") Long bookId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    @Select("SELECT * FROM reading_sessions " +
            "WHERE user_id = #{userId} " +
            "ORDER BY ended_at DESC, started_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<ReadingSession> findUserRecentSessions(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("SELECT * FROM reading_sessions " +
            "WHERE user_id = #{userId} " +
            "AND started_at >= #{since} " +
            "ORDER BY started_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<ReadingSession> findUserSessionsSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("SELECT * FROM reading_sessions " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY started_at DESC")
    List<ReadingSession> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    @Select("SELECT * FROM reading_sessions WHERE id = #{id}")
    ReadingSession findById(@Param("id") Long id);

    @Insert("INSERT INTO reading_sessions " +
            "(user_id, book_id, start_chapter, end_chapter, started_at, " +
            "ended_at, duration_minutes, reading_speed_wpm, device_type, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{startChapter}, #{endChapter}, " +
            "#{startedAt}, #{endedAt}, #{durationMinutes}, #{readingSpeedWpm}, " +
            "#{deviceType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ReadingSession session);

    @Update("UPDATE reading_sessions SET " +
            "ended_at = #{endedAt}, " +
            "end_chapter = #{endChapter}, " +
            "duration_minutes = #{durationMinutes}, " +
            "reading_speed_wpm = #{readingSpeedWpm}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(ReadingSession session);

    @Select("SELECT * FROM reading_sessions " +
            "WHERE user_id = #{userId} AND started_at >= #{start} AND started_at < #{end} " +
            "ORDER BY started_at")
    List<ReadingSession> findUserSessionsBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
