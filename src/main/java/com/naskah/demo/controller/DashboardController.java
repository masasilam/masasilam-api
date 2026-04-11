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
    private final CorrectionService correctionService; // ← tambah inject

    // ═══════════════════════════════════════════════════════════
    // USER DASHBOARD ENDPOINTS (tidak berubah)
    // ═══════════════════════════════════════════════════════════


    // ─────────────────────────────────────────────────────────────────────────
    // MAIN DASHBOARD
    // GET /api/dashboard
    //
    // Dipakai oleh: DashboardOverview.jsx → dashboardService.getMainDashboard()
    //
    // Response: DashboardMainResponse
    //   - overviewStats     : total buku, waktu baca, streak, rating
    //   - booksInProgress   : buku yang sedang dibaca (dari reading_session)
    //   - readingPattern    : pola baca (jam, durasi, pace)
    //   - recentlyRead      : 6 buku terakhir dibaca
    //   - annotationsSummary: ringkasan anotasi EPUB
    //   - recentAchievements: pencapaian terbaru
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<DataResponse<DashboardMainResponse>> getMainDashboard() {
        DataResponse<DashboardMainResponse> response = dashboardService.getMainDashboard();
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIBRARY
    // GET /api/dashboard/library
    //
    // Dipakai oleh: MyLibraryPage.jsx → dashboardService.getLibrary()
    //
    // Query params:
    //   filter : "all" | "reading" | "completed" | "bookmarked"  (default: "all")
    //   page   : halaman (default: 1)
    //   limit  : jumlah per halaman (default: 16)
    //   sortBy : "last_read" | "progress" | "title" | "rating"  (default: "last_read")
    //
    // Response: LibraryPageResponse
    //   - items     : list LibraryBookResponse
    //   - totalData : total buku
    //   - page, limit
    //
    // PERBAIKAN: Sumber data dari reading_session (mencakup EPUB)
    // bukan hanya reading_activity_log.
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // READING HISTORY
    // GET /api/dashboard/history
    //
    // Dipakai oleh: ReadingHistoryPage.jsx → dashboardService.getReadingHistory()
    //
    // Query params:
    //   days  : rentang hari ke belakang (default: 7)
    //   page  : halaman (default: 1)
    //   limit : jumlah per halaman (default: 20)
    //
    // Response: ReadingHistoryPageResponse
    //   - list  : list ReadingHistoryItemResponse (dari reading_session)
    //   - total : total sesi
    //   - page, limit
    //
    // PERBAIKAN: Sebelumnya membaca dari reading_activity_log yang tidak terisi
    // untuk sesi EPUB. Sekarang membaca dari reading_session.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<DataResponse<ReadingHistoryPageResponse>> getReadingHistory(
            @RequestParam(defaultValue = "7")  int days,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int limit) {

        DataResponse<ReadingHistoryPageResponse> response =
                dashboardService.getReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTICS
    // GET /api/dashboard/statistics
    //
    // Dipakai oleh: StatisticsPage.jsx → dashboardService.getStatistics()
    //
    // Query params:
    //   period : jumlah hari ke belakang (default: 30)
    //
    // Response: StatisticsResponse
    //   - totalBooksRead, totalChaptersRead, totalReadingMinutes
    //   - averageReadingSpeedWpm
    //   - readingTimeTrend, completionTrend, speedTrend
    //   - genreBreakdown, peakReadingTimes
    //
    // PERBAIKAN: Semua total dihitung dari reading_session yang mencakup EPUB.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/statistics")
    public ResponseEntity<DataResponse<StatisticsResponse>> getStatistics(
            @RequestParam(defaultValue = "30") int period) {

        DataResponse<StatisticsResponse> response = dashboardService.getStatistics(period);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANNOTATIONS
    // GET /api/dashboard/annotations
    //
    // Dipakai oleh: AnnotationsPage.jsx → dashboardService.getAnnotations()
    //
    // Query params:
    //   type   : "all" | "highlight" | "note" | "bookmark"  (default: "all")
    //   page   : halaman (default: 1)
    //   limit  : jumlah per halaman (default: 20)
    //   sortBy : "recent" | "book"  (default: "recent")
    //
    // Response: AnnotationsPageResponse
    //   - items : list AnnotationItemResponse (dari epub_annotation + epub_bookmark)
    //   - total : total item
    //   - page, limit
    //
    // PERBAIKAN: Sebelumnya membaca dari tabel bookmark/highlight/note yang tidak
    // terisi untuk user yang membaca via EpubReaderPage.
    // Sekarang membaca dari epub_annotation dan epub_bookmark.
    // ─────────────────────────────────────────────────────────────────────────
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

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<UserReviewItemResponse>> getUserReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DatatableResponse<UserReviewItemResponse> response = dashboardService.getUserReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOALS
    // GET /api/dashboard/goals
    //
    // Dipakai oleh: GoalsPage.jsx → dashboardService.getGoals()
    //
    // Response: GoalsResponse
    //   - summary : GoalsSummary (total, completed, active, thisMonth)
    //   - active  : list GoalResponse
    //   - completed: list GoalResponse
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/goals")
    public ResponseEntity<DataResponse<GoalsResponse>> getGoals() {
        DataResponse<GoalsResponse> response = dashboardService.getGoals();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<DataResponse<List<BookRecommendationResponse>>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<BookRecommendationResponse>> response =
                dashboardService.getPersonalizedRecommendations(limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quick-stats")
    public ResponseEntity<DataResponse<QuickStatsResponse>> getQuickStats() {
        DataResponse<QuickStatsResponse> response = dashboardService.getQuickStats();
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALENDAR
    // GET /api/dashboard/calendar
    //
    // Dipakai oleh: CalendarPage.jsx → dashboardService.getCalendar()
    //
    // Query params:
    //   year  : tahun (default: tahun sekarang)
    //   month : bulan 1-12 (default: bulan sekarang)
    //
    // Response: CalendarResponse
    //   - days        : list CalendarDayResponse (aktivitas per hari)
    //   - totalMinutes, totalPages, activeDays
    //
    // PERBAIKAN: Menit baca per hari dihitung dari reading_session,
    // bukan reading_activity_log yang tidak terisi untuk EPUB.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<CalendarResponse>> getCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        int targetYear  = year  != null ? year  : java.time.LocalDate.now().getYear();
        int targetMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();

        DataResponse<CalendarResponse> response = dashboardService.getCalendar(targetYear, targetMonth);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACHIEVEMENTS
    // GET /api/dashboard/achievements
    //
    // Dipakai oleh: AchievementsPage.jsx → dashboardService.getAchievements()
    //
    // Response: AchievementsResponse
    //   - list       : list AchievementResponse
    //   - total      : total pencapaian
    //   - unlocked   : yang sudah terbuka
    //   - categories : list AchievementCategoryResponse
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/achievements")
    public ResponseEntity<DataResponse<AchievementsResponse>> getAchievements() {
        DataResponse<AchievementsResponse> response = dashboardService.getAchievements();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export")
    public ResponseEntity<DataResponse<ExportJobResponse>> exportUserData(
            @RequestParam String format) {
        DataResponse<ExportJobResponse> response =
                dashboardService.exportUserReadingData(format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/corrections")
    public ResponseEntity<DatatableResponse<CorrectionResponse>> getCorrections(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        DatatableResponse<CorrectionResponse> response =
                correctionService.getCorrections(status, page, limit);  // ← nama baru

        return ResponseEntity.ok(response);
    }

    @PostMapping("/corrections/{id}/approve")
    public ResponseEntity<DataResponse<Void>> approveCorrection(
            @PathVariable Long id) {

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