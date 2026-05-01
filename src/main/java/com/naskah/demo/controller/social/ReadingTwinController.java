package com.naskah.demo.controller.social;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;
import com.naskah.demo.service.social.ReadingTwinService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/twins")
@RequiredArgsConstructor
public class ReadingTwinController {

    private final ReadingTwinService twinService;

    /**
     * GET /api/social/twins
     * Temukan "reading twin" kamu — user dengan selera baca paling mirip
     */
    @GetMapping
    public ResponseEntity<DataResponse<List<ReadingTwinResponse>>> getMyTwins(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(twinService.getMyTwins(page, limit));
    }

    /**
     * GET /api/social/twins/{userId}
     * Cek kesamaan selera baca dengan user tertentu
     */
    @GetMapping("/{userId}")
    public ResponseEntity<DataResponse<ReadingTwinResponse>> getTwinWith(
            @PathVariable Long userId) {
        return ResponseEntity.ok(twinService.getTwinWith(userId));
    }

    /**
     * POST /api/social/twins/recalculate
     * Trigger kalkulasi ulang reading twin (async)
     */
    @PostMapping("/recalculate")
    public ResponseEntity<DataResponse<Void>> recalculate() {
        // Fire and forget — async
        return ResponseEntity.ok(new DataResponse<>("Success",
                "Twin calculation started", 200, null));
    }
}