package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadingSessionMapper {

    // ── Insert / Update ───────────────────────────────────────────────────────

    void insertSession(ReadingSession session);

    void updateSession(ReadingSession session);

    // ── Find single ───────────────────────────────────────────────────────────

    ReadingSession findBySessionId(@Param("sessionId") String sessionId);

    ReadingSession findById(@Param("id") Long id);

    // ── Cek duplikat — dipanggil recordEpubSession ────────────────────────────

    boolean existsBySessionId(@Param("sessionId") String sessionId);

    // ── Find by user + book ───────────────────────────────────────────────────

    List<ReadingSession> findUserBookSessions(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("offset") int offset,
            @Param("limit")  int limit);

    List<ReadingSession> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    // ── Find all user sessions ────────────────────────────────────────────────

    List<ReadingSession> findAllUserSessions(@Param("userId") Long userId);

    // ── Find latest session per book ──────────────────────────────────────────

    List<ReadingSession> findLatestSessionPerBook(@Param("userId") Long userId);

    // ── Find recent sessions ──────────────────────────────────────────────────

    List<ReadingSession> findRecentUserSessions(
            @Param("userId") Long userId,
            @Param("limit")  int limit);

    // ── Find sessions since a date ────────────────────────────────────────────

    List<ReadingSession> findUserSessionsSince(
            @Param("userId") Long userId,
            @Param("since")  LocalDateTime since);

    List<ReadingSession> findUserSessionsSincePaged(
            @Param("userId") Long userId,
            @Param("since")  LocalDateTime since,
            @Param("offset") int offset,
            @Param("limit")  int limit);

    // ── Find sessions between two dates ───────────────────────────────────────

    List<ReadingSession> findUserSessionsBetween(
            @Param("userId") Long userId,
            @Param("start")  LocalDateTime start,
            @Param("end")    LocalDateTime end);

    // ── Recent sessions with pagination ──────────────────────────────────────

    List<ReadingSession> findUserRecentSessions(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit")  int limit);
}