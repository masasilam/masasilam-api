package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.EpubBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EpubBookmarkMapper {

    // ── Core CRUD ─────────────────────────────────────────────────────────────

    List<EpubBookmark> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    void insert(EpubBookmark bookmark);

    EpubBookmark findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    // ── Count — dipanggil DashboardServiceImpl ────────────────────────────────

    /** Hitung total bookmark milik user (semua buku). */
    Integer countByUser(@Param("userId") Long userId);

    /** Hitung bookmark milik user untuk satu buku tertentu. */
    Integer countByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    // ── List — dipanggil DashboardServiceImpl.getAnnotations() ───────────────

    /** Semua bookmark user, dengan paginasi. */
    List<EpubBookmark> findByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit")  int limit);

    List<EpubBookmark> findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}