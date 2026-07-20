package com.masasilam.app.controller;

import com.masasilam.app.model.dto.request.RejectCorrectionRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.common.CorrectionService;
import com.masasilam.app.service.common.DashboardService;
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
    private final DashboardService dashboardService;
    private final CorrectionService correctionService;

    @GetMapping
    public ResponseEntity<DataResponse<DashboardMainResponse>> getMainDashboard() {
        DataResponse<DashboardMainResponse> response = dashboardService.getMainDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/library")
    public ResponseEntity<DataResponse<LibraryPageResponse>> getLibrary(@RequestParam(defaultValue = "all") String filter,
                                                                        @RequestParam(defaultValue = "1") int page,
                                                                        @RequestParam(defaultValue = "16") int limit,
                                                                        @RequestParam(defaultValue = "last_read") String sortBy) {
        DataResponse<LibraryPageResponse> response = dashboardService.getLibrary(filter, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<DataResponse<ReadingHistoryPageResponse>> getReadingHistory(@RequestParam(defaultValue = "7") int days,
                                                                                      @RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(defaultValue = "20") int limit) {
        DataResponse<ReadingHistoryPageResponse> response = dashboardService.getReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<DataResponse<StatisticsResponse>> getStatistics(@RequestParam(defaultValue = "30") int period) {
        DataResponse<StatisticsResponse> response = dashboardService.getStatistics(period);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/annotations")
    public ResponseEntity<DataResponse<AnnotationsPageResponse>> getAnnotations(@RequestParam(defaultValue = "all") String type,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "20") int limit,
                                                                                @RequestParam(defaultValue = "recent") String sortBy) {
        DataResponse<AnnotationsPageResponse> response = dashboardService.getAnnotations(type, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<UserReviewItemResponse>> getUserReviews(@RequestParam(defaultValue = "1") int page,
                                                                                    @RequestParam(defaultValue = "10") int limit) {
        DatatableResponse<UserReviewItemResponse> response = dashboardService.getUserReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<DataResponse<List<BookRecommendationResponse>>> getRecommendations(@RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<BookRecommendationResponse>> response = dashboardService.getPersonalizedRecommendations(limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quick-stats")
    public ResponseEntity<DataResponse<QuickStatsResponse>> getQuickStats() {
        DataResponse<QuickStatsResponse> response = dashboardService.getQuickStats();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<CalendarResponse>> getCalendar(@RequestParam(required = false) Integer year,
                                                                      @RequestParam(required = false) Integer month) {
        int targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        int targetMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();
        DataResponse<CalendarResponse> response = dashboardService.getCalendar(targetYear, targetMonth);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/achievements")
    public ResponseEntity<DataResponse<AchievementsResponse>> getAchievements() {
        DataResponse<AchievementsResponse> response = dashboardService.getAchievements();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export")
    public ResponseEntity<DataResponse<ExportJobResponse>> exportUserData(@RequestParam String format) {
        DataResponse<ExportJobResponse> response = dashboardService.exportUserReadingData(format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/corrections")
    public ResponseEntity<DatatableResponse<CorrectionResponse>> getCorrections(@RequestParam(defaultValue = "PENDING") String status,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "20") int limit) {
        DatatableResponse<CorrectionResponse> response = correctionService.getCorrections(status, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/corrections/{id}/approve")
    public ResponseEntity<DataResponse<Void>> approveCorrection(@PathVariable Long id) {
        DataResponse<Void> response = correctionService.approveCorrection(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/corrections/{id}/reject")
    public ResponseEntity<DataResponse<Void>> rejectCorrection(@PathVariable Long id, @RequestBody(required = false) RejectCorrectionRequest request) {
        String note = (request != null) ? request.getNote() : null;
        DataResponse<Void> response = correctionService.rejectCorrection(id, note);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/corrections/my-pending/{slug}")
    public ResponseEntity<DataResponse<List<CorrectionResponse>>> getMyPendingCorrections(@PathVariable String slug) {
        DataResponse<List<CorrectionResponse>> response = correctionService.getMyPendingForBook(slug);
        return ResponseEntity.ok(response);
    }
}