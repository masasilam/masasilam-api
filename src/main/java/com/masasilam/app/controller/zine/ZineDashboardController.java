package com.masasilam.app.controller.zine;

import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.ZineDashboardDTOs.*;
import com.masasilam.app.service.zine.ZineDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ZineDashboardController {
    private final ZineDashboardService zineDashboardService;

    @GetMapping("/zines/library")
    public ResponseEntity<DataResponse<ZineLibraryPageResponse>> getZineLibrary(@RequestParam(defaultValue = "all") String filter,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "16") int limit,
                                                                                @RequestParam(defaultValue = "last_read") String sortBy) {
        DataResponse<ZineLibraryPageResponse> response = zineDashboardService.getZineLibrary(filter, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/zines/history")
    public ResponseEntity<DataResponse<ZineHistoryPageResponse>> getZineReadingHistory(@RequestParam(defaultValue = "7") int days,
                                                                                       @RequestParam(defaultValue = "1") int page,
                                                                                       @RequestParam(defaultValue = "20") int limit) {
        DataResponse<ZineHistoryPageResponse> response = zineDashboardService.getZineReadingHistory(days, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/zines/statistics")
    public ResponseEntity<DataResponse<ZineStatisticsResponse>> getZineStatistics(@RequestParam(defaultValue = "30") int period) {
        DataResponse<ZineStatisticsResponse> response = zineDashboardService.getZineStatistics(period);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/zines/reviews")
    public ResponseEntity<DatatableResponse<UserZineReviewItemResponse>> getUserZineReviews(@RequestParam(defaultValue = "1") int page,
                                                                                            @RequestParam(defaultValue = "10") int limit) {
        DatatableResponse<UserZineReviewItemResponse> response = zineDashboardService.getUserZineReviews(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/combined-overview")
    public ResponseEntity<DataResponse<CombinedOverviewStats>> getCombinedOverview() {
        DataResponse<CombinedOverviewStats> response = zineDashboardService.getCombinedOverviewStats();
        return ResponseEntity.ok(response);
    }
}