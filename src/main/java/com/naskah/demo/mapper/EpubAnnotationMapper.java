package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.EpubAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EpubAnnotationMapper {

    // ── Core CRUD (query di XML) ──────────────────────────────────────────────

    List<EpubAnnotation> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    void insert(EpubAnnotation annotation);

    EpubAnnotation findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    // ── Dashboard methods (query di XML) ─────────────────────────────────────

    Integer countByUser(@Param("userId") Long userId);

    List<EpubAnnotation> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    List<EpubAnnotation> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    List<EpubAnnotation> findByUser(@Param("userId") Long userId);
}