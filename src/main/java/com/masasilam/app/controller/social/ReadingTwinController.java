package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.ReadingTwinService;
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

    @GetMapping
    public ResponseEntity<DataResponse<List<ReadingTwinResponse>>> getMyTwins(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                              @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(twinService.getMyTwins(page, limit));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<DataResponse<ReadingTwinResponse>> getTwinWith(@PathVariable Long userId) {
        return ResponseEntity.ok(twinService.getTwinWith(userId));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<DataResponse<Void>> recalculate() {
        return ResponseEntity.ok(new DataResponse<>("Success", "Twin calculation started", 200, null));
    }
}