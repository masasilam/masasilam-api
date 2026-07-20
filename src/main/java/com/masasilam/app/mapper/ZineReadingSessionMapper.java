package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.ZineReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ZineReadingSessionMapper {
    void insert(ZineReadingSession session);
    void update(ZineReadingSession session);

    ZineReadingSession findById(Long id);
    List<ZineReadingSession> findAllUserSessions(Long userId);
    List<ZineReadingSession> findLatestSessionPerZine(Long userId);
    List<ZineReadingSession> findRecentUserSessions(@Param("userId") Long userId, @Param("limit") int limit);
    List<ZineReadingSession> findUserSessionsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    List<ZineReadingSession> findUserSessionsBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
    ZineReadingSession findLatestByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}