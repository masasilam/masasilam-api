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
    public ResponseEntity<DatatableResponse<ReadingGroupResponse>> getPublicGroups(@RequestParam(required = false) String search,
                                                                                   @RequestParam(required = false) String focusType,
                                                                                   @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                   @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getPublicGroups(search, focusType, page, limit));
    }

    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingGroupResponse>> getMyGroups(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                               @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getMyGroups(page, limit));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<ReadingGroupResponse>> getGroupDetail(@PathVariable String slug) {
        return ResponseEntity.ok(groupService.getGroupDetail(slug));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<DataResponse<ReadingGroupResponse>> updateGroup(@PathVariable Long groupId,
                                                                          @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<DataResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.deleteGroup(groupId));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<DataResponse<Void>> joinGroup(@PathVariable Long groupId,
                                                        @RequestBody(required = false) JoinGroupRequest request) {
        return ResponseEntity.ok(groupService.joinGroup(groupId, request != null ? request : new JoinGroupRequest()));
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<DataResponse<Void>> leaveGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.leaveGroup(groupId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<DatatableResponse<GroupMemberResponse>> getMembers(@PathVariable Long groupId,
                                                                             @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                             @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getMembers(groupId, page, limit));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<DataResponse<Void>> kickMember(@PathVariable Long groupId,
                                                         @PathVariable Long userId) {
        return ResponseEntity.ok(groupService.kickMember(groupId, userId));
    }

    @PutMapping("/{groupId}/members/{userId}/role")
    public ResponseEntity<DataResponse<Void>> promoteMember(@PathVariable Long groupId,
                                                            @PathVariable Long userId,
                                                            @RequestParam String role) {
        return ResponseEntity.ok(groupService.promoteMember(groupId, userId, role));
    }

    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<DatatableResponse<GroupJoinRequestResponse>> getPendingRequests(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getPendingRequests(groupId));
    }

    @PutMapping("/{groupId}/join-requests/{requestId}")
    public ResponseEntity<DataResponse<Void>> reviewJoinRequest(@PathVariable Long groupId,
                                                                @PathVariable Long requestId,
                                                                @Valid @RequestBody ReviewJoinRequestRequest request) {
        return ResponseEntity.ok(groupService.reviewJoinRequest(groupId, requestId, request));
    }

    @PutMapping("/{groupId}/current-read")
    public ResponseEntity<DataResponse<GroupReadingScheduleResponse>> setCurrentRead(@PathVariable Long groupId,
                                                                                     @Valid @RequestBody SetCurrentReadRequest request) {
        return ResponseEntity.ok(groupService.setCurrentRead(groupId, request));
    }

    @GetMapping("/{groupId}/schedules")
    public ResponseEntity<DatatableResponse<GroupReadingScheduleResponse>> getSchedules(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getSchedules(groupId));
    }

    @PostMapping("/{groupId}/schedules")
    public ResponseEntity<DataResponse<GroupReadingScheduleResponse>> createSchedule(@PathVariable Long groupId,
                                                                                     @Valid @RequestBody CreateScheduleRequest request) {
        DataResponse<GroupReadingScheduleResponse> response = groupService.createSchedule(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{groupId}/schedules/{scheduleId}/complete")
    public ResponseEntity<DataResponse<Void>> completeSchedule(@PathVariable Long groupId,
                                                               @PathVariable Long scheduleId) {
        return ResponseEntity.ok(groupService.completeSchedule(groupId, scheduleId));
    }

    @GetMapping("/{groupId}/discussions")
    public ResponseEntity<DatatableResponse<GroupDiscussionResponse>> getDiscussions(@PathVariable Long groupId,
                                                                                     @RequestParam(required = false) Long scheduleId,
                                                                                     @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                     @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(groupService.getDiscussions(groupId, scheduleId, page, limit));
    }

    @PostMapping("/{groupId}/discussions")
    public ResponseEntity<DataResponse<GroupDiscussionResponse>> createDiscussion(@PathVariable Long groupId,
                                                                                  @Valid @RequestBody CreateDiscussionRequest request) {
        DataResponse<GroupDiscussionResponse> response = groupService.createDiscussion(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{groupId}/discussions/{discussionId}")
    public ResponseEntity<DataResponse<GroupDiscussionResponse>> updateDiscussion(@PathVariable Long groupId,
                                                                                  @PathVariable Long discussionId,
                                                                                  @Valid @RequestBody CreateDiscussionRequest request) {
        return ResponseEntity.ok(groupService.updateDiscussion(groupId, discussionId, request));
    }

    @DeleteMapping("/{groupId}/discussions/{discussionId}")
    public ResponseEntity<DataResponse<Void>> deleteDiscussion(@PathVariable Long groupId,
                                                               @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.deleteDiscussion(groupId, discussionId));
    }

    @PostMapping("/{groupId}/discussions/{discussionId}/like")
    public ResponseEntity<DataResponse<Void>> likeDiscussion(@PathVariable Long groupId,
                                                             @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.likeDiscussion(groupId, discussionId));
    }

    @DeleteMapping("/{groupId}/discussions/{discussionId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeDiscussion(@PathVariable Long groupId,
                                                               @PathVariable Long discussionId) {
        return ResponseEntity.ok(groupService.unlikeDiscussion(groupId, discussionId));
    }

    @GetMapping("/{groupId}/polls")
    public ResponseEntity<DatatableResponse<GroupPollResponse>> getPolls(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getPolls(groupId));
    }

    @PostMapping("/{groupId}/polls")
    public ResponseEntity<DataResponse<GroupPollResponse>> createPoll(@PathVariable Long groupId,
                                                                      @Valid @RequestBody CreatePollRequest request) {
        DataResponse<GroupPollResponse> response = groupService.createPoll(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{groupId}/polls/{pollId}/vote")
    public ResponseEntity<DataResponse<GroupPollResponse>> votePoll(@PathVariable Long groupId,
                                                                    @PathVariable Long pollId,
                                                                    @Valid @RequestBody VotePollRequest request) {
        return ResponseEntity.ok(groupService.votePoll(groupId, pollId, request));
    }
}