package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.EpubAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EpubAnnotationMapper {

    // ── Core CRUD ─────────────────────────────────────────────────────────────

    List<EpubAnnotation> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    void insert(EpubAnnotation annotation);

    EpubAnnotation findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    // ── Count methods (Sudah dilengkapi) ──────────────────────────────────────

    Integer countByUser(@Param("userId") Long userId);

    Integer countHighlightsByUser(@Param("userId") Long userId);

    Integer countNotesByUser(@Param("userId") Long userId);

    Integer countByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    // ── List & Pagination methods ─────────────────────────────────────────────

    // Digunakan untuk halaman utama anotasi dengan limit & offset
    List<EpubAnnotation> findByUserPaged(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<EpubAnnotation> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    List<EpubAnnotation> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    // Untuk Library/Lainnya (Tanpa Pagination)
    List<EpubAnnotation> findByUser(@Param("userId") Long userId);

    List<EpubAnnotation> findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}