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

    @GetMapping
    public ResponseEntity<DataResponse<UserReadingDashboardResponse>> getMainDashboard() {
        DataResponse<UserReadingDashboardResponse> response = dashboardService.getUserReadingDashboard();
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/history")
    public ResponseEntity<DatatableResponse<ReadingActivityResponse>> getReadingHistory(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        DatatableResponse<ReadingActivityResponse> response =
                dashboardService.getReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<DataResponse<ReadingStatisticsResponse>> getReadingStatistics(
            @RequestParam(defaultValue = "30") int period) {
        DataResponse<ReadingStatisticsResponse> response =
                dashboardService.getReadingStatistics(period);
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<UserReviewItemResponse>> getUserReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DatatableResponse<UserReviewItemResponse> response =
                dashboardService.getUserReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/goals")
    public ResponseEntity<DataResponse<ReadingGoalsResponse>> getReadingGoals() {
        DataResponse<ReadingGoalsResponse> response = dashboardService.getReadingGoals();
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

    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<ReadingCalendarResponse>> getReadingCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        DataResponse<ReadingCalendarResponse> response =
                dashboardService.getReadingCalendar(year, month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/achievements")
    public ResponseEntity<DataResponse<List<AchievementResponse>>> getAchievements() {
        DataResponse<List<AchievementResponse>> response =
                dashboardService.getUserAchievements();
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