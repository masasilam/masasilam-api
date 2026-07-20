package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.ReadingGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/groups")
@RequiredArgsConstructor
public class ReadingGroupController {
    private final ReadingGroupService groupService;

    @PostMapping
    public ResponseEntity<DataResponse<ReadingGroupResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        DataResponse<ReadingGroupResponse> response = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<ReadingGroupResponse>> getPublicGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String focusType,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getPublicGroups(search, focusType, page, limit));
    }

    /**
     * GET /api/social/groups/me
     * Grup yang saya ikuti
     */
    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingGroupResponse>> getMyGroups(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getMyGroups(page, limit));
    }

    /**
     * GET /api/social/groups/{slug}
     * Detail grup by slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<ReadingGroupResponse>> getGroupDetail(
            @PathVariable String slug) {
        return ResponseEntity.ok(groupService.getGroupDetail(slug));
    }

    /**
     * PUT /api/social/groups/{groupId}
     * Update grup
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<DataResponse<ReadingGroupResponse>> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    /**
     * DELETE /api/social/groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<DataResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.deleteGroup(groupId));
    }

    // ── MEMBERSHIP ────────────────────────────────────────────────────────────

    /**
     * POST /api/social/groups/{groupId}/join
     * Bergabung ke grup (atau kirim request jika private)
     */
    @PostMapping("/{groupId}/join")
    public ResponseEntity<DataResponse<Void>> joinGroup(
            @PathVariable Long groupId,
            @RequestBody(required = false) JoinGroupRequest request) {
        return ResponseEntity.ok(groupService.joinGroup(groupId,
                request != null ? request : new JoinGroupRequest()));
    }

    /**
     * DELETE /api/social/groups/{groupId}/leave
     * Keluar dari grup
     */
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<DataResponse<Void>> leaveGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.leaveGroup(groupId));
    }

    /**
     * GET /api/social/groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<DatatableResponse<GroupMemberResponse>> getMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getMembers(groupId, page, limit));
    }

    /**
     * DELETE /api/social/groups/{groupId}/members/{userId}
     * Kick member (owner/moderator only)
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<DataResponse<Void>> kickMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(groupService.kickMember(groupId, userId));
    }

    /**
     * PUT /api/social/groups/{groupId}/members/{userId}/role
     * Ubah role member (owner only)
     * Query param: role=moderator|member
     */
    @PutMapping("/{groupId}/members/{userId}/role")
    public ResponseEntity<DataResponse<Void>> promoteMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestParam String role) {
        return ResponseEntity.ok(groupService.promoteMember(groupId, userId, role));
    }

    // ── JOIN REQUESTS ─────────────────────────────────────────────────────────

    /**
     * GET /api/social/groups/{groupId}/join-requests
     * Daftar request bergabung (owner/moderator only)
     */
    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<DatatableResponse<GroupJoinRequestResponse>> getPendingRequests(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getPendingRequests(groupId));
    }

    /**
     * PUT /api/social/groups/{groupId}/join-requests/{requestId}
     * Approve/reject join request
     */
    @PutMapping("/{groupId}/join-requests/{requestId}")
    public ResponseEntity<DataResponse<Void>> reviewJoinRequest(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewJoinRequestRequest request) {
        return ResponseEntity.ok(groupService.reviewJoinRequest(groupId, requestId, request));
    }

    // ── SCHEDULES ─────────────────────────────────────────────────────────────

    /**
     * PUT /api/social/groups/{groupId}/current-read
     * Set buku/konten yang sedang dibaca grup saat ini
     */
    @PutMapping("/{groupId}/current-read")
    public ResponseEntity<DataResponse<GroupReadingScheduleResponse>> setCurrentRead(
            @PathVariable Long groupId,
            @Valid @RequestBody SetCurrentReadRequest request) {
        return ResponseEntity.ok(groupService.setCurrentRead(groupId, request));
    }

    /**
     * GET /api/social/groups/{groupId}/schedules
     * Semua jadwal baca grup
     */
    @GetMapping("/{groupId}/schedules")
    public ResponseEntity<DatatableResponse<GroupReadingScheduleResponse>> getSchedules(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getSchedules(groupId));
    }

    /**
     * POST /api/social/groups/{groupId}/schedules
     * Tambah jadwal baca baru
     */
    @PostMapping("/{groupId}/schedules")
    public ResponseEntity<DataResponse<GroupReadingScheduleResponse>> createSchedule(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateScheduleRequest request) {
        DataResponse<GroupReadingScheduleResponse> response =
                groupService.createSchedule(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/social/groups/{groupId}/schedules/{scheduleId}/complete
     * Tandai jadwal selesai
     */
    @PutMapping("/{groupId}/schedules/{scheduleId}/complete")
    public ResponseEntity<DataResponse<Void>> completeSchedule(
            @PathVariable Long groupId,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(groupService.completeSchedule(groupId, scheduleId));
    }

    // ── DISCUSSIONS ───────────────────────────────────────────────────────────

    /**
     * GET /api/social/groups/{groupId}/discussions
     * Diskusi dalam grup, bisa filter per jadwal
     */
    @GetMapping("/{groupId}/discussions")
    public ResponseEntity<DatatableResponse<GroupDiscussionResponse>> getDiscussions(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getDiscussions(groupId, scheduleId, page, limit));
    }

    /**
     * POST /api/social/groups/{groupId}/discussions
     * Buat postingan diskusi baru (atau reply jika ada parentId)
     */
    @PostMapping("/{groupId}/discussions")
    public ResponseEntity<DataResponse<GroupDiscussionResponse>> createDiscussion(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateDiscussionRequest request) {
        DataResponse<GroupDiscussionResponse> response =
                groupService.createDiscussion(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/social/groups/{groupId}/discussions/{discussionId}
     */
    @PutMapping("/{groupId}/discussions/{discussionId}")
    public ResponseEntity<DataResponse<GroupDiscussionResponse>> updateDiscussion(
            @PathVariable Long groupId,
            @PathVariable Long discussionId,
            @Valid @RequestBody CreateDiscussionRequest request) {
        return ResponseEntity.ok(groupService.updateDiscussion(groupId, discussionId, request));
    }

    /**
     * DELETE /api/social/groups/{groupId}/discussions/{discussionId}
     */
    @DeleteMapping("/{groupId}/discussions/{discussionId}")
    public ResponseEntity<DataResponse<Void>> deleteDiscussion(
            @PathVariable Long groupId,
            @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.deleteDiscussion(groupId, discussionId));
    }

    /**
     * POST /api/social/groups/{groupId}/discussions/{discussionId}/like
     */
    @PostMapping("/{groupId}/discussions/{discussionId}/like")
    public ResponseEntity<DataResponse<Void>> likeDiscussion(
            @PathVariable Long groupId,
            @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.likeDiscussion(groupId, discussionId));
    }

    /**
     * DELETE /api/social/groups/{groupId}/discussions/{discussionId}/like
     */
    @DeleteMapping("/{groupId}/discussions/{discussionId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeDiscussion(
            @PathVariable Long groupId,
            @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.unlikeDiscussion(groupId, discussionId));
    }

    // ── POLLS ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/social/groups/{groupId}/polls
     */
    @GetMapping("/{groupId}/polls")
    public ResponseEntity<DatatableResponse<GroupPollResponse>> getPolls(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getPolls(groupId));
    }

    /**
     * POST /api/social/groups/{groupId}/polls
     * Buat poll baru (owner/moderator only)
     */
    @PostMapping("/{groupId}/polls")
    public ResponseEntity<DataResponse<GroupPollResponse>> createPoll(
            @PathVariable Long groupId,
            @Valid @RequestBody CreatePollRequest request) {
        DataResponse<GroupPollResponse> response = groupService.createPoll(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/social/groups/{groupId}/polls/{pollId}/vote
     * Vote pada poll
     */
    @PostMapping("/{groupId}/polls/{pollId}/vote")
    public ResponseEntity<DataResponse<GroupPollResponse>> votePoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @Valid @RequestBody VotePollRequest request) {
        return ResponseEntity.ok(groupService.votePoll(groupId, pollId, request));
    }
}