package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.RejectCorrectionRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.CorrectionService;
import com.naskah.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService  dashboardService;
    private final CorrectionService correctionService;

    // ═══════════════════════════════════════════════════════════
    // MAIN DASHBOARD
    // GET /api/dashboard
    //
    // Response: DashboardMainResponse
    //   - overviewStats      : total buku, waktu baca, streak, rating
    //   - booksInProgress    : buku yang sedang dibaca
    //   - zinесInProgress    : zine yang sedang dibaca          ← BARU
    //   - readingPattern     : pola baca (jam, durasi, pace)
    //   - recentlyRead       : 6 buku+zine terakhir dibaca
    //   - annotationsSummary : ringkasan anotasi EPUB
    //   - recentAchievements : pencapaian terbaru
    // ═══════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<DataResponse<DashboardMainResponse>> getMainDashboard() {
        DataResponse<DashboardMainResponse> response = dashboardService.getMainDashboard();
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // LIBRARY (buku)
    // GET /api/dashboard/library
    // params: filter, page, limit, sortBy
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/library")
    public ResponseEntity<DataResponse<LibraryPageResponse>> getLibrary(
            @RequestParam(defaultValue = "all")       String filter,
            @RequestParam(defaultValue = "1")         int page,
            @RequestParam(defaultValue = "16")        int limit,
            @RequestParam(defaultValue = "last_read") String sortBy) {

        DataResponse<LibraryPageResponse> response =
                dashboardService.getLibrary(filter, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // READING HISTORY (buku)
    // GET /api/dashboard/history
    // params: days, page, limit
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/history")
    public ResponseEntity<DataResponse<ReadingHistoryPageResponse>> getReadingHistory(
            @RequestParam(defaultValue = "7")  int days,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int limit) {

        DataResponse<ReadingHistoryPageResponse> response =
                dashboardService.getReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS (buku)
    // GET /api/dashboard/statistics
    // params: period
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<DataResponse<StatisticsResponse>> getStatistics(
            @RequestParam(defaultValue = "30") int period) {

        DataResponse<StatisticsResponse> response = dashboardService.getStatistics(period);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // ANNOTATIONS (buku + zine — keduanya pakai epub_annotation)
    // GET /api/dashboard/annotations
    // params: type, page, limit, sortBy
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/annotations")
    public ResponseEntity<DataResponse<AnnotationsPageResponse>> getAnnotations(
            @RequestParam(defaultValue = "all")    String type,
            @RequestParam(defaultValue = "1")      int page,
            @RequestParam(defaultValue = "20")     int limit,
            @RequestParam(defaultValue = "recent") String sortBy) {

        DataResponse<AnnotationsPageResponse> response =
                dashboardService.getAnnotations(type, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEWS (buku)
    // GET /api/dashboard/reviews
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<UserReviewItemResponse>> getUserReviews(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int limit) {
        DatatableResponse<UserReviewItemResponse> response =
                dashboardService.getUserReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // RECOMMENDATIONS
    // GET /api/dashboard/recommendations
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/recommendations")
    public ResponseEntity<DataResponse<List<BookRecommendationResponse>>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<BookRecommendationResponse>> response =
                dashboardService.getPersonalizedRecommendations(limit);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // QUICK STATS
    // GET /api/dashboard/quick-stats
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/quick-stats")
    public ResponseEntity<DataResponse<QuickStatsResponse>> getQuickStats() {
        DataResponse<QuickStatsResponse> response = dashboardService.getQuickStats();
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // CALENDAR
    // GET /api/dashboard/calendar
    // params: year, month
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<CalendarResponse>> getCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        int targetYear  = year  != null ? year  : java.time.LocalDate.now().getYear();
        int targetMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();

        DataResponse<CalendarResponse> response =
                dashboardService.getCalendar(targetYear, targetMonth);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // ACHIEVEMENTS
    // GET /api/dashboard/achievements
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/achievements")
    public ResponseEntity<DataResponse<AchievementsResponse>> getAchievements() {
        DataResponse<AchievementsResponse> response = dashboardService.getAchievements();
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT
    // POST /api/dashboard/export
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/export")
    public ResponseEntity<DataResponse<ExportJobResponse>> exportUserData(
            @RequestParam String format) {
        DataResponse<ExportJobResponse> response =
                dashboardService.exportUserReadingData(format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // ═══════════════════════════════════════════════════════════
    // CORRECTIONS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/corrections")
    public ResponseEntity<DatatableResponse<CorrectionResponse>> getCorrections(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1")       int page,
            @RequestParam(defaultValue = "20")      int limit) {

        DatatableResponse<CorrectionResponse> response =
                correctionService.getCorrections(status, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/corrections/{id}/approve")
    public ResponseEntity<DataResponse<Void>> approveCorrection(@PathVariable Long id) {
        DataResponse<Void> response = correctionService.approveCorrection(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/corrections/{id}/reject")
    public ResponseEntity<DataResponse<Void>> rejectCorrection(
            @PathVariable Long id,
            @RequestBody(required = false) RejectCorrectionRequest request) {
        String note = (request != null) ? request.getNote() : null;
        DataResponse<Void> response = correctionService.rejectCorrection(id, note);
        return ResponseEntity.ok(response);
    }
}