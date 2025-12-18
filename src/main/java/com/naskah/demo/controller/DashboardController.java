package com.naskah.demo.controller;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DashboardController - User Reading Dashboard
 *
 * Menampilkan overview lengkap aktivitas membaca user:
 * - Total buku, waktu baca, completion
 * - Buku yang sedang dibaca
 * - Riwayat baca terbaru
 * - Statistik dan pattern membaca
 * - Quick access ke annotations
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * ═══════════════════════════════════════════════════════════
     * MAIN DASHBOARD - Overview lengkap aktivitas membaca user
     * ═══════════════════════════════════════════════════════════
     *
     * Data yang ditampilkan:
     * ✅ Total Buku (yang pernah dibaca)
     * ✅ Waktu Baca (total jam membaca)
     * ✅ Selesai Dibaca (completion count)
     * ✅ Rating Rata-rata (average rating yang user berikan)
     * ✅ Terakhir Dibaca (recent books dengan progress)
     * ✅ Buku Sedang Dibaca (in-progress books)
     * ✅ Reading Streak (hari berturut-turut)
     * ✅ Annotations Summary (bookmarks, highlights, notes)
     *
     * GET /api/dashboard
     */
    @GetMapping
    public ResponseEntity<DataResponse<UserReadingDashboardResponse>> getMainDashboard() {
        DataResponse<UserReadingDashboardResponse> response =
                dashboardService.getUserReadingDashboard();
        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * PERPUSTAKAAN SAYA - All books user has interacted with
     * ═══════════════════════════════════════════════════════════
     *
     * Menampilkan semua buku yang:
     * - Pernah dibaca (ada reading progress)
     * - Di-bookmark
     * - Di-highlight
     * - Di-note
     * - Di-rating/review
     *
     * Dengan filter:
     * - reading: sedang dibaca
     * - completed: sudah selesai
     * - bookmarked: ada bookmark
     * - all: semua
     *
     * GET /api/dashboard/library?filter=reading&page=1&limit=12&sortBy=last_read
     */
    @GetMapping("/library")
    public ResponseEntity<DatatableResponse<BookLibraryItemResponse>> getUserLibrary(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "last_read") String sortBy) {

        DatatableResponse<BookLibraryItemResponse> response =
                dashboardService.getUserLibrary(filter, page, limit, sortBy);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * RIWAYAT BACA - Reading activity timeline
     * ═══════════════════════════════════════════════════════════
     *
     * Menampilkan timeline aktivitas:
     * - Mulai membaca buku
     * - Selesai membaca chapter
     * - Menambahkan bookmark/highlight/note
     * - Memberikan rating/review
     *
     * GET /api/dashboard/history?days=7&page=1&limit=20
     */
    @GetMapping("/history")
    public ResponseEntity<DatatableResponse<ReadingActivityResponse>> getReadingHistory(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        DatatableResponse<ReadingActivityResponse> response =
                dashboardService.getReadingHistory(days, page, limit);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * STATISTIK MEMBACA - Reading statistics & analytics
     * ═══════════════════════════════════════════════════════════
     *
     * Menampilkan:
     * - Daily/weekly reading time chart
     * - Books completed per month
     * - Reading speed trend
     * - Favorite genres
     * - Reading patterns (time of day, day of week)
     *
     * GET /api/dashboard/stats?period=30
     */
    @GetMapping("/stats")
    public ResponseEntity<DataResponse<ReadingStatisticsResponse>> getReadingStatistics(
            @RequestParam(defaultValue = "30") int period) {

        DataResponse<ReadingStatisticsResponse> response =
                dashboardService.getReadingStatistics(period);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * ANNOTATIONS OVERVIEW - All user annotations
     * ═══════════════════════════════════════════════════════════
     *
     * GET /api/dashboard/annotations?type=all&page=1&limit=20
     *
     * Types: all, bookmarks, highlights, notes
     */
    @GetMapping("/annotations")
    public ResponseEntity<DatatableResponse<AnnotationItemResponse>> getAllAnnotations(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "recent") String sortBy) {

        DatatableResponse<AnnotationItemResponse> response =
                dashboardService.getAllAnnotations(type, page, limit, sortBy);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * REVIEWS OVERVIEW - All user reviews
     * ═══════════════════════════════════════════════════════════
     *
     * GET /api/dashboard/reviews?page=1&limit=10
     */
    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<UserReviewItemResponse>> getUserReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        DatatableResponse<UserReviewItemResponse> response =
                dashboardService.getUserReviews(page, limit);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * READING GOALS - User's reading goals & progress
     * ═══════════════════════════════════════════════════════════
     *
     * GET /api/dashboard/goals
     */
    @GetMapping("/goals")
    public ResponseEntity<DataResponse<ReadingGoalsResponse>> getReadingGoals() {
        DataResponse<ReadingGoalsResponse> response =
                dashboardService.getReadingGoals();

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * RECOMMENDATIONS - Personalized book recommendations
     * ═══════════════════════════════════════════════════════════
     *
     * Based on:
     * - User's reading history
     * - Favorite genres
     * - Ratings
     * - Similar readers
     *
     * GET /api/dashboard/recommendations?limit=10
     */
    @GetMapping("/recommendations")
    public ResponseEntity<DataResponse<List<BookRecommendationResponse>>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {

        DataResponse<List<BookRecommendationResponse>> response =
                dashboardService.getPersonalizedRecommendations(limit);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * QUICK STATS - Lightweight stats for widgets
     * ═══════════════════════════════════════════════════════════
     *
     * GET /api/dashboard/quick-stats
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<DataResponse<QuickStatsResponse>> getQuickStats() {
        DataResponse<QuickStatsResponse> response =
                dashboardService.getQuickStats();

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * READING CALENDAR - Calendar view of reading activity
     * ═══════════════════════════════════════════════════════════
     *
     * Contribution-style calendar showing:
     * - Days with reading activity
     * - Reading time per day
     * - Streaks
     *
     * GET /api/dashboard/calendar?year=2025&month=1
     */
    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<ReadingCalendarResponse>> getReadingCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        DataResponse<ReadingCalendarResponse> response =
                dashboardService.getReadingCalendar(year, month);

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * ACHIEVEMENTS - User achievements & badges
     * ═══════════════════════════════════════════════════════════
     *
     * Achievements like:
     * - Read 10 books
     * - 7-day streak
     * - 100 hours reading
     * - etc.
     *
     * GET /api/dashboard/achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<DataResponse<List<AchievementResponse>>> getAchievements() {
        DataResponse<List<AchievementResponse>> response =
                dashboardService.getUserAchievements();

        return ResponseEntity.ok(response);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * EXPORT DATA - Export user's reading data
     * ═══════════════════════════════════════════════════════════
     *
     * Export formats:
     * - JSON: complete data export
     * - CSV: reading log
     * - PDF: reading report
     *
     * POST /api/dashboard/export
     */
    @PostMapping("/export")
    public ResponseEntity<DataResponse<ExportJobResponse>> exportUserData(
            @RequestParam String format) {

        DataResponse<ExportJobResponse> response =
                dashboardService.exportUserReadingData(format);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}