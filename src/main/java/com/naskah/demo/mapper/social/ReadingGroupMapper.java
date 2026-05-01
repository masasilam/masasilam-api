package com.naskah.demo.mapper.social;

import com.naskah.demo.model.entity.social.*;
import com.naskah.demo.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReadingGroupMapper {
    void insertGroup(ReadingGroup group);
    void updateGroup(ReadingGroup group);
    void softDeleteGroup(Long id);
    ReadingGroup findById(Long id);
    ReadingGroup findBySlug(String slug);
    ReadingGroupResponse getGroupDetail(@Param("groupId") Long groupId, @Param("currentUserId") Long currentUserId);

    List<ReadingGroupResponse> findPublicGroups(@Param("search") String search, @Param("focusType") String focusType,
                                                @Param("currentUserId") Long currentUserId,
                                                @Param("offset") int offset, @Param("limit") int limit);
    List<ReadingGroupResponse> findUserGroups(@Param("userId") Long userId,
                                              @Param("offset") int offset, @Param("limit") int limit);
    int countPublicGroups(@Param("search") String search, @Param("focusType") String focusType);
    int countUserGroups(Long userId);

    // Members
    void insertMember(ReadingGroupMember member);
    void updateMember(ReadingGroupMember member);
    ReadingGroupMember findMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    List<GroupMemberResponse> findMembers(@Param("groupId") Long groupId,
                                          @Param("offset") int offset, @Param("limit") int limit);
    int countMembers(Long groupId);
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    String getMemberRole(@Param("groupId") Long groupId, @Param("userId") Long userId);

    // Schedules
    void insertSchedule(GroupReadingSchedule schedule);
    void updateSchedule(GroupReadingSchedule schedule);
    GroupReadingSchedule findScheduleById(Long id);
    List<GroupReadingScheduleResponse> findSchedulesByGroup(@Param("groupId") Long groupId);
    GroupReadingScheduleResponse findActiveSchedule(Long groupId);
    void markScheduleCompleted(Long id);

    // Discussions
    void insertDiscussion(GroupDiscussion discussion);
    void updateDiscussion(GroupDiscussion discussion);
    void softDeleteDiscussion(Long id);
    GroupDiscussion findDiscussionById(Long id);
    List<GroupDiscussionResponse> findDiscussionsByGroup(@Param("groupId") Long groupId,
                                                         @Param("scheduleId") Long scheduleId,
                                                         @Param("currentUserId") Long currentUserId,
                                                         @Param("offset") int offset, @Param("limit") int limit);
    List<GroupDiscussionResponse> findRepliesByParent(@Param("parentId") Long parentId,
                                                      @Param("currentUserId") Long currentUserId);
    int countDiscussionsByGroup(@Param("groupId") Long groupId, @Param("scheduleId") Long scheduleId);
    void incrementDiscussionLikeCount(Long id);
    void decrementDiscussionLikeCount(Long id);
    void incrementReplyCount(Long id);
    void decrementReplyCount(Long id);

    // Discussion likes
    void insertDiscussionLike(@Param("discussionId") Long discussionId, @Param("userId") Long userId);
    void deleteDiscussionLike(@Param("discussionId") Long discussionId, @Param("userId") Long userId);
    boolean isDiscussionLiked(@Param("discussionId") Long discussionId, @Param("userId") Long userId);

    // Join requests
    void insertJoinRequest(GroupJoinRequest request);
    void updateJoinRequest(GroupJoinRequest request);
    GroupJoinRequest findJoinRequest(@Param("groupId") Long groupId, @Param("userId") Long userId);
    List<GroupJoinRequestResponse> findPendingRequests(@Param("groupId") Long groupId);

    // Polls
    void insertPoll(GroupPoll poll);
    void updatePollOptions(@Param("pollId") Long pollId, @Param("options") String options);
    void closePoll(Long pollId);
    GroupPoll findPollById(Long id);
    List<GroupPollResponse> findPollsByGroup(@Param("groupId") Long groupId, @Param("currentUserId") Long currentUserId);
    void insertPollVote(GroupPollVote vote);
    GroupPollVote findPollVote(@Param("pollId") Long pollId, @Param("userId") Long userId);
    void deletePollVote(@Param("pollId") Long pollId, @Param("userId") Long userId);

    // Leaderboard (progress within group)
    List<GroupMemberResponse> getMemberProgressLeaderboard(@Param("groupId") Long groupId,
                                                           @Param("entityType") String entityType,
                                                           @Param("entityId") Long entityId);
}