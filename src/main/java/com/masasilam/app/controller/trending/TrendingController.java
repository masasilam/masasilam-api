package com.masasilam.app.controller.trending;

import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.TrendingItemResponse;
import com.masasilam.app.service.trending.TrendingService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping
    public ResponseEntity<DataResponse<List<TrendingItemResponse>>> getTrending(@RequestParam String type, @RequestParam(defaultValue = "10") @Min(1) int limit) {
        List<TrendingItemResponse> data = trendingService.getTrendingByType(type, limit);
        return ResponseEntity.ok(new DataResponse<>("Success", "Trending retrieved successfully", 200, data));
    }

    @GetMapping("/categories")
    public ResponseEntity<DataResponse<Map<String, List<TrendingItemResponse>>>> getTrendingPerCategory() {
        Map<String, List<TrendingItemResponse>> data = trendingService.getTrendingPerCategory();
        return ResponseEntity.ok(new DataResponse<>("Success", "Trending per category retrieved successfully", 200, data));
    }

    @GetMapping("/overall")
    public ResponseEntity<DataResponse<List<TrendingItemResponse>>> getTrendingOverall(@RequestParam(defaultValue = "15") @Min(1) int limit) {
        List<TrendingItemResponse> data = trendingService.getTrendingOverall(limit);
        return ResponseEntity.ok(new DataResponse<>("Success", "Overall trending retrieved successfully", 200, data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<DataResponse<String>> refreshTrending() {
        trendingService.refreshAll();
        return ResponseEntity.ok(new DataResponse<>("Success", "Trending refresh triggered", 200, "OK"));
    }
}