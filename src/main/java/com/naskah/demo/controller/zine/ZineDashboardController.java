package com.naskah.demo.controller.zine;

import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.DatatableResponse;
import com.naskah.demo.model.dto.response.ZineDashboardDTOs.*;
import com.naskah.demo.service.zine.ZineDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Semua endpoint dashboard yang berkaitan dengan Zine.
 * Base path: /api/dashboard/zines
 *
 * ─────────────────────────────────────────────────────────────
 * PEMETAAN ENDPOINT → FRONTEND
 * ─────────────────────────────────────────────────────────────
 *
 * GET  /api/dashboard/zines/library
 *      → ZineLibraryPage.jsx
 *      params: filter (all/reading/completed/bookmarked),
 *              page, limit, sortBy (last_read/progress/title)
 *
 * GET  /api/dashboard/zines/history
 *      → ZineReadingHistoryPage.jsx
 *      params: days (default 7), page, limit
 *
 * GET  /api/dashboard/zines/statistics
 *      → ZineStatisticsPage.jsx
 *      params: period (default 30, dalam hari)
 *
 * GET  /api/dashboard/zines/reviews
 *      → (tab review di dashboard user)
 *      params: page, limit
 *
 * GET  /api/dashboard/combined-overview
 *      → DashboardOverview.jsx (menggantikan / melengkapi /api/dashboard)
 *      Mengembalikan stats gabungan buku + zine.
 * ─────────────────────────────────────────────────────────────
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ZineDashboardController {

    private final ZineDashboardService zineDashboardService;

    // ─── LIBRARY ──────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/zines/library
     *
     * Mengembalikan daftar zine yang pernah dibaca user,
     * beserta progress, status baca, highlight & bookmark count.
     */
    @GetMapping("/zines/library")
    public ResponseEntity<DataResponse<ZineLibraryPageResponse>> getZineLibrary(
            @RequestParam(defaultValue = "all")       String filter,
            @RequestParam(defaultValue = "1")         int page,
            @RequestParam(defaultValue = "16")        int limit,
            @RequestParam(defaultValue = "last_read") String sortBy) {

        DataResponse<ZineLibraryPageResponse> response =
                zineDashboardService.getZineLibrary(filter, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    // ─── HISTORY ──────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/zines/history
     *
     * Riwayat sesi baca zine user dalam N hari terakhir.
     * Setiap item menampilkan zine, chapter, durasi, dan CFI terakhir.
     */
    @GetMapping("/zines/history")
    public ResponseEntity<DataResponse<ZineHistoryPageResponse>> getZineReadingHistory(
            @RequestParam(defaultValue = "7")  int days,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int limit) {

        DataResponse<ZineHistoryPageResponse> response =
                zineDashboardService.getZineReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    // ─── STATISTICS ───────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/zines/statistics
     *
     * Statistik baca zine: total menit, chapter, estimasi WPM,
     * genre breakdown, peak hours, tren antar periode.
     */
    @GetMapping("/zines/statistics")
    public ResponseEntity<DataResponse<ZineStatisticsResponse>> getZineStatistics(
            @RequestParam(defaultValue = "30") int period) {

        DataResponse<ZineStatisticsResponse> response =
                zineDashboardService.getZineStatistics(period);
        return ResponseEntity.ok(response);
    }

    // ─── USER ZINE REVIEWS ────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/zines/reviews
     *
     * Daftar review yang pernah ditulis user untuk zine.
     */
    @GetMapping("/zines/reviews")
    public ResponseEntity<DatatableResponse<UserZineReviewItemResponse>> getUserZineReviews(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int limit) {

        DatatableResponse<UserZineReviewItemResponse> response =
                zineDashboardService.getUserZineReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    // ─── COMBINED OVERVIEW ────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/combined-overview
     *
     * Stats gabungan buku + zine dalam satu response.
     * Digunakan oleh DashboardOverview.jsx untuk menampilkan
     * total waktu baca, streak, dan completion rate secara terpadu.
     */
    @GetMapping("/combined-overview")
    public ResponseEntity<DataResponse<CombinedOverviewStats>> getCombinedOverview() {
        DataResponse<CombinedOverviewStats> response =
                zineDashboardService.getCombinedOverviewStats();
        return ResponseEntity.ok(response);
    }
}