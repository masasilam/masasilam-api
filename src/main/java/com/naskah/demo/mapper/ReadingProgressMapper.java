package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingProgressMapper {

    // ── Sudah ada — dipakai DashboardServiceImpl ──────────────────────────────

    ReadingProgress findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    List<ReadingProgress> findAllByUser(
            @Param("userId") Long userId
    );

    void updateReadingProgress(ReadingProgress overallProgress);

    // ── TAMBAHKAN jika belum ada ───────────────────────────────────────────────

    /** Insert baris baru. ID di-generate DB (useGeneratedKeys di XML). */
    void insert(ReadingProgress progress);

    /**
     * Update penuh: percentage_completed, status, current_page,
     * reading_time_minutes, last_read_at, completed_at.
     * WHERE id = #{id}
     */
    void update(ReadingProgress progress);

    /**
     * Update hanya waktu baca + last_read_at.
     * Dipakai saat progress tidak maju (user ulang baca halaman yang sama).
     * WHERE id = #{id}
     */
    void updateTimeOnly(ReadingProgress progress);
}