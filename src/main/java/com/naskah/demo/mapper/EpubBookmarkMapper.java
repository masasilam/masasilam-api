package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.EpubBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EpubBookmarkMapper {

    // ── Core CRUD (query di XML) ──────────────────────────────────────────────

    List<EpubBookmark> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    void insert(EpubBookmark bookmark);

    EpubBookmark findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    // ── Dashboard methods (query di XML) ─────────────────────────────────────

    Integer countByUser(@Param("userId") Long userId);

    List<EpubBookmark> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    List<EpubBookmark> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    List<EpubBookmark> findByUser(@Param("userId") Long userId);
}