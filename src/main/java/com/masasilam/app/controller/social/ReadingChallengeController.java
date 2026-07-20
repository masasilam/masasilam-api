package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.request.social.CreateChallengeRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.ReadingChallengeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/challenges")
@RequiredArgsConstructor
public class ReadingChallengeController {
    private final ReadingChallengeService challengeService;

    @GetMapping
    public ResponseEntity<DatatableResponse<ReadingChallengeResponse>> getActiveChallenges(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                           @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getActiveChallenges(page, limit));
    }

    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingChallengeResponse>> getMyChallenges(@RequestParam(required = false) String status,
                                                                                       @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                       @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getMyChallenges(status, page, limit));
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> getChallengeDetail(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getChallengeDetail(challengeId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> getChallengeBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(challengeService.getChallengeBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> createChallenge(@Valid @RequestBody CreateChallengeRequest request) {
        DataResponse<ReadingChallengeResponse> response = challengeService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{challengeId}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> updateChallenge(@PathVariable Long challengeId,
                                                                                  @Valid @RequestBody CreateChallengeRequest request) {
        return ResponseEntity.ok(challengeService.updateChallenge(challengeId, request));
    }

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<DataResponse<Void>> deleteChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.deleteChallenge(challengeId));
    }

    @PostMapping("/{challengeId}/join")
    public ResponseEntity<DataResponse<Void>> joinChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.joinChallenge(challengeId));
    }

    @DeleteMapping("/{challengeId}/join")
    public ResponseEntity<DataResponse<Void>> abandonChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.abandonChallenge(challengeId));
    }

    @PostMapping("/{challengeId}/progress")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> recordProgress(@PathVariable Long challengeId,
                                                                                 @RequestParam String entityType,
                                                                                 @RequestParam Long entityId) {
        return ResponseEntity.ok(challengeService.recordProgress(challengeId, entityType, entityId));
    }

    @GetMapping("/{challengeId}/leaderboard")
    public ResponseEntity<DataResponse<ChallengeLeaderboardResponse>> getLeaderboard(@PathVariable Long challengeId,
                                                                                     @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                     @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getLeaderboard(challengeId, page, limit));
    }
}