package com.masasilam.app.mapper.reading;

import com.masasilam.app.model.entity.ReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadingSessionMapper {
    void insertSession(ReadingSession session);
    void updateSession(ReadingSession session);
    ReadingSession findBySessionId(@Param("sessionId") String sessionId);
    ReadingSession findById(@Param("id") Long id);
    boolean existsBySessionId(@Param("sessionId") String sessionId);
    List<ReadingSession> findUserBookSessions(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("offset") int offset, @Param("limit") int limit);
    List<ReadingSession> findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<ReadingSession> findAllUserSessions(@Param("userId") Long userId);
    List<ReadingSession> findLatestSessionPerBook(@Param("userId") Long userId);
    List<ReadingSession> findRecentUserSessions(@Param("userId") Long userId, @Param("limit") int limit);
    List<ReadingSession> findUserSessionsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    List<ReadingSession> findUserSessionsSincePaged(@Param("userId") Long userId, @Param("since") LocalDateTime since, @Param("offset") int offset, @Param("limit") int limit);
    List<ReadingSession> findUserSessionsBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<ReadingSession> findUserRecentSessions(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}