package com.naskah.app.model.dto.response;

import com.naskah.app.model.dto.response.ZineDashboardDTOs.ZineInProgressResponse;
import lombok.Data;

import java.util.List;

/**
 * Response utama dashboard.
 * Menggabungkan data buku DAN zine agar frontend bisa
 * menampilkan semuanya di satu halaman overview.
 */
@Data
public class DashboardMainResponse {

    /** Statistik overview: total buku+zine, waktu baca, streak, rating */
    private OverviewStats overviewStats;

    /** Buku yang sedang dalam proses baca (progress < 95%) */
    private List<BookInProgressResponse> booksInProgress;

    /**
     * Zine yang sedang dalam proses baca (progress < 95%).
     * Diisi oleh DashboardServiceImpl menggunakan ZineReadingSessionMapper
     * dan ZineReadingProgressMapper.
     */
    private List<ZineInProgressResponse> zinesInProgress;

    /** Pola baca: jam favorit, kecepatan, pace */
    private ReadingPatternSummary readingPattern;

    /**
     * Buku dan zine yang baru-baru ini dibaca.
     * Diurutkan berdasarkan waktu sesi terakhir (gabungan).
     */
    private List<RecentlyReadResponse> recentlyRead;

    /** Ringkasan anotasi dari epub_annotation dan epub_bookmark */
    private AnnotationsSummary annotationsSummary;

    /** Pencapaian terbaru (placeholder) */
    private List<Object> recentAchievements;
}