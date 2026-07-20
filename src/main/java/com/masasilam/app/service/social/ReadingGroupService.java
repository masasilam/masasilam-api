package com.masasilam.app.service.social;

import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;

public interface ReadingGroupService {
    DataResponse<ReadingGroupResponse> createGroup(CreateGroupRequest request);
    DataResponse<ReadingGroupResponse> updateGroup(Long groupId, UpdateGroupRequest request);
    DataResponse<Void> deleteGroup(Long groupId);
    DataResponse<ReadingGroupResponse> getGroupDetail(String slug);
    DatatableResponse<ReadingGroupResponse> getPublicGroups(String search, String focusType, int page, int limit);
    DatatableResponse<ReadingGroupResponse> getMyGroups(int page, int limit);
    DataResponse<Void> joinGroup(Long groupId, JoinGroupRequest request);
    DataResponse<Void> leaveGroup(Long groupId);
    DataResponse<Void> kickMember(Long groupId, Long userId);
    DataResponse<Void> promoteMember(Long groupId, Long userId, String role);
    DatatableResponse<GroupMemberResponse> getMembers(Long groupId, int page, int limit);
    DataResponse<GroupReadingScheduleResponse> setCurrentRead(Long groupId, SetCurrentReadRequest request);
    DataResponse<GroupReadingScheduleResponse> createSchedule(Long groupId, CreateScheduleRequest request);
    DatatableResponse<GroupReadingScheduleResponse> getSchedules(Long groupId);
    DataResponse<Void> completeSchedule(Long groupId, Long scheduleId);
    DataResponse<GroupDiscussionResponse> createDiscussion(Long groupId, CreateDiscussionRequest request);
    DataResponse<GroupDiscussionResponse> updateDiscussion(Long groupId, Long discussionId, CreateDiscussionRequest request);
    DataResponse<Void> deleteDiscussion(Long groupId, Long discussionId);
    DatatableResponse<GroupDiscussionResponse> getDiscussions(Long groupId, Long scheduleId, int page, int limit);
    DataResponse<Void> likeDiscussion(Long groupId, Long discussionId);
    DataResponse<Void> unlikeDiscussion(Long groupId, Long discussionId);
    DataResponse<GroupPollResponse> createPoll(Long groupId, CreatePollRequest request);
    DataResponse<GroupPollResponse> votePoll(Long groupId, Long pollId, VotePollRequest request);
    DatatableResponse<GroupPollResponse> getPolls(Long groupId);
    DatatableResponse<GroupJoinRequestResponse> getPendingRequests(Long groupId);
    DataResponse<Void> reviewJoinRequest(Long groupId, Long requestId, ReviewJoinRequestRequest request);
}