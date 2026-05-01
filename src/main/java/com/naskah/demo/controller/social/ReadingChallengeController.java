package com.naskah.demo.controller.social;

import com.naskah.demo.model.dto.request.social.CreateChallengeRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;
import com.naskah.demo.service.social.ReadingChallengeService;
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

    // ── DISCOVERY ─────────────────────────────────────────────────────────────

    /**
     * GET /api/social/challenges
     * Semua tantangan yang aktif
     */
    @GetMapping
    public ResponseEntity<DatatableResponse<ReadingChallengeResponse>> getActiveChallenges(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getActiveChallenges(page, limit));
    }

    /**
     * GET /api/social/challenges/me
     * Tantangan yang saya ikuti
     */
    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingChallengeResponse>> getMyChallenges(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getMyChallenges(status, page, limit));
    }

    /**
     * GET /api/social/challenges/{challengeId}
     * Detail tantangan by ID
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> getChallengeDetail(
            @PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getChallengeDetail(challengeId));
    }

    /**
     * GET /api/social/challenges/slug/{slug}
     * Detail tantangan by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> getChallengeBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(challengeService.getChallengeBySlug(slug));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/social/challenges
     * Buat tantangan baru
     */
    @PostMapping
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> createChallenge(
            @Valid @RequestBody CreateChallengeRequest request) {
        DataResponse<ReadingChallengeResponse> response =
                challengeService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/social/challenges/{challengeId}
     */
    @PutMapping("/{challengeId}")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> updateChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody CreateChallengeRequest request) {
        return ResponseEntity.ok(challengeService.updateChallenge(challengeId, request));
    }

    /**
     * DELETE /api/social/challenges/{challengeId}
     */
    @DeleteMapping("/{challengeId}")
    public ResponseEntity<DataResponse<Void>> deleteChallenge(
            @PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.deleteChallenge(challengeId));
    }

    // ── PARTICIPATION ─────────────────────────────────────────────────────────

    /**
     * POST /api/social/challenges/{challengeId}/join
     * Ikut tantangan
     */
    @PostMapping("/{challengeId}/join")
    public ResponseEntity<DataResponse<Void>> joinChallenge(
            @PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.joinChallenge(challengeId));
    }

    /**
     * DELETE /api/social/challenges/{challengeId}/join
     * Tinggalkan tantangan
     */
    @DeleteMapping("/{challengeId}/join")
    public ResponseEntity<DataResponse<Void>> abandonChallenge(
            @PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.abandonChallenge(challengeId));
    }

    /**
     * POST /api/social/challenges/{challengeId}/progress
     * Catat progres manual (tambahkan buku/konten yang sudah dibaca)
     * Query params: entityType, entityId
     */
    @PostMapping("/{challengeId}/progress")
    public ResponseEntity<DataResponse<ReadingChallengeResponse>> recordProgress(
            @PathVariable Long challengeId,
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(
                challengeService.recordProgress(challengeId, entityType, entityId));
    }

    // ── LEADERBOARD ───────────────────────────────────────────────────────────

    /**
     * GET /api/social/challenges/{challengeId}/leaderboard
     */
    @GetMapping("/{challengeId}/leaderboard")
    public ResponseEntity<DataResponse<ChallengeLeaderboardResponse>> getLeaderboard(
            @PathVariable Long challengeId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(challengeService.getLeaderboard(challengeId, page, limit));
    }
}