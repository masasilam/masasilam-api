package com.masasilam.app.service.social.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.mapper.social.ReadingGroupMapper;
import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.service.social.*;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingGroupServiceImpl implements ReadingGroupService {
    private final ReadingGroupMapper groupMapper;
    private final UserMapper userMapper;
    private final ActivityFeedService feedService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String OWNER = "owner";
    private static final String MODERATOR = "moderator";
    private static final String MEMBER = "member";
    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";
    private static final String PENDING = "pending";
    private static final String APPROVE = "approve";

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long currentUserIdOrNull() {
        try {
            String u = headerHolder.getUsername();
            if (u == null || u.isBlank()) return null;
            User user = userMapper.findUserByUsername(u);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void requireGroupRole(Long groupId, Long userId, String... roles) {
        String myRole = groupMapper.getMemberRole(groupId, userId);
        if (myRole == null) throw new ForbiddenException();
        boolean ok = false;
        for (String r : roles) {
            if (r.equals(myRole)) {
                ok = true;
                break;
            }
        }
        if (!ok) throw new ForbiddenException();
    }

    @Override
    @Transactional
    public DataResponse<ReadingGroupResponse> createGroup(CreateGroupRequest request) {
        User me = requireAuth();

        String slug = generateSlug(request.getName());
        String finalSlug = slug;
        int c = 2;
        while (groupMapper.findBySlug(finalSlug) != null) finalSlug = slug + "-" + c++;

        ReadingGroup group = new ReadingGroup();
        group.setOwnerId(me.getId());
        group.setName(request.getName());
        group.setSlug(finalSlug);
        group.setDescription(request.getDescription());
        group.setGroupType(request.getGroupType() != null ? request.getGroupType() : PUBLIC);
        group.setFocusType(request.getFocusType() != null ? request.getFocusType() : "mixed");
        group.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 100);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            String tagsArray = request.getTags().stream()
                    .map(tag -> "\"" + tag.replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(",", "{", "}"));
            group.setTags(tagsArray);
        } else {
            group.setTags(null);
        }

        group.setRules(request.getRules());
        groupMapper.insertGroup(group);

        ReadingGroupMember member = new ReadingGroupMember();
        member.setGroupId(group.getId());
        member.setUserId(me.getId());
        member.setRole(OWNER);
        groupMapper.insertMember(member);

        feedService.publishActivity(me.getId(), "created_group", "GROUP",
                group.getId(), finalSlug, request.getName(), null, "{}", PUBLIC);

        ReadingGroupResponse response = groupMapper.getGroupDetail(group.getId(), me.getId());
        List<GroupMemberResponse> members = groupMapper.findMembers(group.getId(), 0, 5);
        response.setRecentMembers(members);
        return new DataResponse<>(SUCCESS, "Group created", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<ReadingGroupResponse> updateGroup(Long groupId, UpdateGroupRequest request) {
        User me = requireAuth();
        ReadingGroup group = groupMapper.findById(groupId);
        if (group == null) throw new DataNotFoundException();

        String role = groupMapper.getMemberRole(groupId, me.getId());
        if (!OWNER.equals(role) && !MODERATOR.equals(role)) {
            throw new ForbiddenException();
        }

        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        if (request.getGroupType() != null) group.setGroupType(request.getGroupType());
        if (request.getMaxMembers() != null) group.setMaxMembers(request.getMaxMembers());

        if (request.getTags() != null) {
            if (!request.getTags().isEmpty()) {
                String tagsArray = request.getTags().stream()
                        .map(tag -> "\"" + tag.replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",", "{", "}"));
                group.setTags(tagsArray);
            } else {
                group.setTags(null);
            }
        }

        if (request.getRules() != null) group.setRules(request.getRules());

        groupMapper.updateGroup(group);

        ReadingGroupResponse response = groupMapper.getGroupDetail(groupId, me.getId());
        return new DataResponse<>(SUCCESS, "Group updated", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteGroup(Long groupId) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER);
        groupMapper.softDeleteGroup(groupId);
        return new DataResponse<>(SUCCESS, "Group deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<ReadingGroupResponse> getGroupDetail(String slug) {
        Long currentUserId = currentUserIdOrNull();
        ReadingGroup group = groupMapper.findBySlug(slug);
        if (group == null) throw new DataNotFoundException();

        ReadingGroupResponse response = groupMapper.getGroupDetail(group.getId(), currentUserId);
        List<GroupMemberResponse> members = groupMapper.findMembers(group.getId(), 0, 6);
        response.setRecentMembers(members);
        response.setActiveSchedule(groupMapper.findActiveSchedule(group.getId()));
        return new DataResponse<>(SUCCESS, "Group retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DatatableResponse<ReadingGroupResponse> getPublicGroups(String search, String focusType, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<ReadingGroupResponse> groups = groupMapper.findPublicGroups(search, focusType, currentUserId, offset, limit);
        int total = groupMapper.countPublicGroups(search, focusType);
        return new DatatableResponse<>(SUCCESS, "Groups retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, groups));
    }

    @Override
    public DatatableResponse<ReadingGroupResponse> getMyGroups(int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<ReadingGroupResponse> groups = groupMapper.findUserGroups(me.getId(), offset, limit);
        int total = groupMapper.countUserGroups(me.getId());
        return new DatatableResponse<>(SUCCESS, "My groups retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, groups));
    }

    @Override
    @Transactional
    public DataResponse<Void> joinGroup(Long groupId, JoinGroupRequest request) {
        User me = requireAuth();
        ReadingGroup group = groupMapper.findById(groupId);
        if (group == null) throw new DataNotFoundException();
        if (groupMapper.isMember(groupId, me.getId()))
            throw new BadRequestException("Already a member");
        if (group.getMemberCount() >= group.getMaxMembers())
            throw new BadRequestException("Group is full");

        if (PRIVATE.equals(group.getGroupType())) {
            GroupJoinRequest joinReq = groupMapper.findJoinRequest(groupId, me.getId());
            if (joinReq != null && PENDING.equals(joinReq.getStatus()))
                throw new BadRequestException("Join request already pending");

            GroupJoinRequest newReq = new GroupJoinRequest();
            newReq.setGroupId(groupId);
            newReq.setUserId(me.getId());
            newReq.setMessage(request.getMessage());
            groupMapper.insertJoinRequest(newReq);

            notificationService.sendNotification(group.getOwnerId(), me.getId(),
                    "group_join_request", "GROUP", groupId,
                    me.getUsername() + " ingin bergabung ke grup \"" + group.getName() + "\"", "{}");

            return new DataResponse<>(SUCCESS, "Join request sent", HttpStatus.OK.value(), null);
        }

        ReadingGroupMember member = new ReadingGroupMember();
        member.setGroupId(groupId);
        member.setUserId(me.getId());
        member.setRole(MEMBER);
        groupMapper.insertMember(member);

        feedService.publishActivity(me.getId(), "joined_group", "GROUP",
                groupId, group.getSlug(), group.getName(), group.getCoverImageUrl(), "{}", PUBLIC);

        notificationService.sendNotification(group.getOwnerId(), me.getId(),
                "group_join_request", "GROUP", groupId,
                me.getUsername() + " bergabung ke grup \"" + group.getName() + "\"", "{}");

        return new DataResponse<>(SUCCESS, "Joined group successfully", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> leaveGroup(Long groupId) {
        User me = requireAuth();
        ReadingGroupMember member = groupMapper.findMember(groupId, me.getId());
        if (member == null) throw new DataNotFoundException();
        if (OWNER.equals(member.getRole()))
            throw new BadRequestException("Owner cannot leave. Transfer ownership or delete the group.");
        member.setIsActive(false);
        groupMapper.updateMember(member);
        return new DataResponse<>(SUCCESS, "Left group", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> kickMember(Long groupId, Long userId) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);
        ReadingGroupMember target = groupMapper.findMember(groupId, userId);
        if (target == null) throw new DataNotFoundException();
        if (OWNER.equals(target.getRole())) throw new ForbiddenException();
        target.setIsActive(false);
        groupMapper.updateMember(target);
        return new DataResponse<>(SUCCESS, "Member removed", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> promoteMember(Long groupId, Long userId, String role) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER);
        ReadingGroupMember target = groupMapper.findMember(groupId, userId);
        if (target == null) throw new DataNotFoundException();
        target.setRole(role);
        groupMapper.updateMember(target);
        return new DataResponse<>(SUCCESS, "Member role updated", HttpStatus.OK.value(), null);
    }

    @Override
    public DatatableResponse<GroupMemberResponse> getMembers(Long groupId, int page, int limit) {
        int offset = (page - 1) * limit;
        List<GroupMemberResponse> members = groupMapper.findMembers(groupId, offset, limit);
        int total = groupMapper.countMembers(groupId);
        return new DatatableResponse<>(SUCCESS, "Members retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, members));
    }

    @Override
    @Transactional
    public DataResponse<GroupReadingScheduleResponse> setCurrentRead(Long groupId, SetCurrentReadRequest request) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);
        ReadingGroup group = groupMapper.findById(groupId);
        if (group == null) throw new DataNotFoundException();

        group.setCurrentReadEntityType(request.getEntityType());
        group.setCurrentReadEntityId(request.getEntityId());
        group.setCurrentReadStart(request.getStartDate());
        group.setCurrentReadEnd(request.getEndDate());
        groupMapper.updateGroup(group);

        GroupReadingSchedule schedule = new GroupReadingSchedule();
        schedule.setGroupId(groupId);
        schedule.setEntityType(request.getEntityType());
        schedule.setEntityId(request.getEntityId());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        groupMapper.insertSchedule(schedule);

        return new DataResponse<>(SUCCESS, "Current read updated",
                HttpStatus.OK.value(), groupMapper.findActiveSchedule(groupId));
    }

    @Override
    @Transactional
    public DataResponse<GroupReadingScheduleResponse> createSchedule(Long groupId, CreateScheduleRequest request) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);

        GroupReadingSchedule schedule = new GroupReadingSchedule();
        schedule.setGroupId(groupId);
        schedule.setEntityType(request.getEntityType());
        schedule.setEntityId(request.getEntityId());
        schedule.setChapterStart(request.getChapterStart());
        schedule.setChapterEnd(request.getChapterEnd());
        schedule.setChapterLabel(request.getChapterLabel());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setDiscussionPrompt(request.getDiscussionPrompt());
        groupMapper.insertSchedule(schedule);

        GroupReadingSchedule saved = groupMapper.findScheduleById(schedule.getId());
        GroupReadingScheduleResponse response = new GroupReadingScheduleResponse();
        response.setId(saved.getId());
        response.setGroupId(groupId);
        response.setEntityType(saved.getEntityType());
        response.setEntityId(saved.getEntityId());
        response.setChapterStart(saved.getChapterStart());
        response.setChapterEnd(saved.getChapterEnd());
        response.setChapterLabel(saved.getChapterLabel());
        response.setStartDate(saved.getStartDate());
        response.setEndDate(saved.getEndDate());
        response.setDiscussionPrompt(saved.getDiscussionPrompt());
        response.setIsCompleted(false);
        return new DataResponse<>(SUCCESS, "Schedule created", HttpStatus.CREATED.value(), response);
    }

    @Override
    public DatatableResponse<GroupReadingScheduleResponse> getSchedules(Long groupId) {
        List<GroupReadingScheduleResponse> schedules = groupMapper.findSchedulesByGroup(groupId);
        return new DatatableResponse<>(SUCCESS, "Schedules retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(1, 100, schedules.size(), schedules));
    }

    @Override
    @Transactional
    public DataResponse<Void> completeSchedule(Long groupId, Long scheduleId) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);
        groupMapper.markScheduleCompleted(scheduleId);
        return new DataResponse<>(SUCCESS, "Schedule marked complete", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<GroupDiscussionResponse> createDiscussion(Long groupId, CreateDiscussionRequest request) {
        User me = requireAuth();
        if (!groupMapper.isMember(groupId, me.getId())) throw new ForbiddenException();

        GroupDiscussion discussion = new GroupDiscussion();
        discussion.setGroupId(groupId);
        discussion.setScheduleId(request.getScheduleId());
        discussion.setUserId(me.getId());
        discussion.setParentId(request.getParentId());
        discussion.setTitle(request.getTitle());
        discussion.setContent(request.getContent());
        discussion.setEntityType(request.getEntityType());
        discussion.setEntityId(request.getEntityId());
        discussion.setCfiReference(request.getCfiReference());
        groupMapper.insertDiscussion(discussion);

        if (request.getParentId() != null) {
            groupMapper.incrementReplyCount(request.getParentId());
        }

        ReadingGroup group = groupMapper.findById(groupId);
        if (group != null && !group.getOwnerId().equals(me.getId())) {
            notificationService.sendNotification(group.getOwnerId(), me.getId(),
                    "group_discussion", "GROUP_DISCUSSION", discussion.getId(),
                    me.getUsername() + " memulai diskusi baru di grup \"" + group.getName() + "\"",
                    "{}");
        }

        List<GroupDiscussionResponse> responses = groupMapper.findDiscussionsByGroup(groupId, null, me.getId(), 0, 1);
        GroupDiscussionResponse resp = responses.isEmpty() ? new GroupDiscussionResponse() : responses.get(0);
        return new DataResponse<>(SUCCESS, "Discussion created", HttpStatus.CREATED.value(), resp);
    }

    @Override
    @Transactional
    public DataResponse<GroupDiscussionResponse> updateDiscussion(Long groupId, Long discussionId, CreateDiscussionRequest request) {
        User me = requireAuth();
        GroupDiscussion disc = groupMapper.findDiscussionById(discussionId);
        if (disc == null || disc.getIsDeleted()) throw new DataNotFoundException();
        if (!disc.getUserId().equals(me.getId())) throw new ForbiddenException();
        disc.setContent(request.getContent());
        groupMapper.updateDiscussion(disc);

        List<GroupDiscussionResponse> responses = groupMapper.findDiscussionsByGroup(groupId, null, me.getId(), 0, 50);
        GroupDiscussionResponse resp = responses.stream()
                .filter(r -> r.getId().equals(discussionId)).findFirst()
                .orElse(new GroupDiscussionResponse());
        return new DataResponse<>(SUCCESS, "Discussion updated", HttpStatus.OK.value(), resp);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteDiscussion(Long groupId, Long discussionId) {
        User me = requireAuth();
        GroupDiscussion disc = groupMapper.findDiscussionById(discussionId);
        if (disc == null) throw new DataNotFoundException();
        boolean isMod = OWNER.equals(groupMapper.getMemberRole(groupId, me.getId()))
                || MODERATOR.equals(groupMapper.getMemberRole(groupId, me.getId()));
        if (!disc.getUserId().equals(me.getId()) && !isMod) throw new ForbiddenException();
        groupMapper.softDeleteDiscussion(discussionId);
        if (disc.getParentId() != null) groupMapper.decrementReplyCount(disc.getParentId());
        return new DataResponse<>(SUCCESS, "Discussion deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DatatableResponse<GroupDiscussionResponse> getDiscussions(Long groupId, Long scheduleId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<GroupDiscussionResponse> discussions = groupMapper.findDiscussionsByGroup(groupId, scheduleId, currentUserId, offset, limit);

        discussions.forEach(d -> {
            List<GroupDiscussionResponse> replies = groupMapper.findRepliesByParent(d.getId(), currentUserId);
            d.setReplies(replies);
        });

        int total = groupMapper.countDiscussionsByGroup(groupId, scheduleId);
        return new DatatableResponse<>(SUCCESS, "Discussions retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, discussions));
    }

    @Override
    @Transactional
    public DataResponse<Void> likeDiscussion(Long groupId, Long discussionId) {
        User me = requireAuth();
        if (!groupMapper.isMember(groupId, me.getId())) throw new ForbiddenException();
        if (groupMapper.isDiscussionLiked(discussionId, me.getId()))
            throw new BadRequestException("Already liked");
        groupMapper.insertDiscussionLike(discussionId, me.getId());
        groupMapper.incrementDiscussionLikeCount(discussionId);
        return new DataResponse<>(SUCCESS, "Discussion liked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unlikeDiscussion(Long groupId, Long discussionId) {
        User me = requireAuth();
        if (!groupMapper.isDiscussionLiked(discussionId, me.getId())) throw new DataNotFoundException();
        groupMapper.deleteDiscussionLike(discussionId, me.getId());
        groupMapper.decrementDiscussionLikeCount(discussionId);
        return new DataResponse<>(SUCCESS, "Discussion unliked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<GroupPollResponse> createPoll(Long groupId, CreatePollRequest request) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);

        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < request.getOptions().size(); i++) {
            options.add(Map.of("id", i + 1, "text", request.getOptions().get(i), "vote_count", 0));
        }
        String optionsJson;
        try {
            optionsJson = objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            optionsJson = "[]";
        }

        GroupPoll poll = new GroupPoll();
        poll.setGroupId(groupId);
        poll.setCreatedBy(me.getId());
        poll.setQuestion(request.getQuestion());
        poll.setOptions(optionsJson);
        poll.setEndsAt(request.getEndsAt());
        groupMapper.insertPoll(poll);

        List<GroupPollResponse> polls = groupMapper.findPollsByGroup(groupId, me.getId());
        GroupPollResponse response = polls.stream()
                .filter(p -> p.getId().equals(poll.getId())).findFirst()
                .orElse(new GroupPollResponse());
        return new DataResponse<>(SUCCESS, "Poll created", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<GroupPollResponse> votePoll(Long groupId, Long pollId, VotePollRequest request) {
        User me = requireAuth();
        if (!groupMapper.isMember(groupId, me.getId())) throw new ForbiddenException();

        GroupPoll poll = groupMapper.findPollById(pollId);
        if (poll == null || poll.getIsClosed()) throw new BadRequestException("Poll is closed or not found");

        GroupPollVote existing = groupMapper.findPollVote(pollId, me.getId());
        if (existing != null) {
            try {
                List<Map<String, Object>> options = objectMapper.readValue(poll.getOptions(), List.class);
                options.forEach(opt -> {
                    int id = (int) opt.get("id");
                    if (id == existing.getOptionId())
                        opt.put("vote_count", Math.max(0, (int) opt.get("vote_count") - 1));
                    if (id == request.getOptionId()) opt.put("vote_count", (int) opt.get("vote_count") + 1);
                });
                groupMapper.updatePollOptions(pollId, objectMapper.writeValueAsString(options));
            } catch (Exception ignored) {
            }
            groupMapper.deletePollVote(pollId, me.getId());
        } else {
            try {
                List<Map<String, Object>> options = objectMapper.readValue(poll.getOptions(), List.class);
                options.forEach(opt -> {
                    if ((int) opt.get("id") == request.getOptionId())
                        opt.put("vote_count", (int) opt.get("vote_count") + 1);
                });
                groupMapper.updatePollOptions(pollId, objectMapper.writeValueAsString(options));
            } catch (Exception ignored) {
            }
        }

        GroupPollVote vote = new GroupPollVote();
        vote.setPollId(pollId);
        vote.setUserId(me.getId());
        vote.setOptionId(request.getOptionId());
        groupMapper.insertPollVote(vote);

        List<GroupPollResponse> polls = groupMapper.findPollsByGroup(groupId, me.getId());
        GroupPollResponse response = polls.stream()
                .filter(p -> p.getId().equals(pollId)).findFirst()
                .orElse(new GroupPollResponse());
        return new DataResponse<>(SUCCESS, "Vote recorded", HttpStatus.OK.value(), response);
    }

    @Override
    public DatatableResponse<GroupPollResponse> getPolls(Long groupId) {
        Long currentUserId = currentUserIdOrNull();
        List<GroupPollResponse> polls = groupMapper.findPollsByGroup(groupId, currentUserId);
        return new DatatableResponse<>(SUCCESS, "Polls retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(1, 100, polls.size(), polls));
    }

    @Override
    public DatatableResponse<GroupJoinRequestResponse> getPendingRequests(Long groupId) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);
        List<GroupJoinRequestResponse> requests = groupMapper.findPendingRequests(groupId);
        return new DatatableResponse<>(SUCCESS, "Requests retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(1, 100, requests.size(), requests));
    }

    @Override
    @Transactional
    public DataResponse<Void> reviewJoinRequest(Long groupId, Long requestId, ReviewJoinRequestRequest request) {
        User me = requireAuth();
        requireGroupRole(groupId, me.getId(), OWNER, MODERATOR);

        GroupJoinRequest joinReq = new GroupJoinRequest();
        joinReq.setId(requestId);
        joinReq.setStatus(request.getAction());
        joinReq.setReviewedBy(me.getId());
        groupMapper.updateJoinRequest(joinReq);

        if (APPROVE.equals(request.getAction())) {
            List<GroupJoinRequestResponse> pending = groupMapper.findPendingRequests(groupId);
            GroupJoinRequest entity = new GroupJoinRequest();
            entity.setGroupId(groupId);
            ReadingGroupMember member = new ReadingGroupMember();
            member.setGroupId(groupId);
            member.setRole(MEMBER);
            groupMapper.insertMember(member);

            ReadingGroup group = groupMapper.findById(groupId);
            notificationService.sendNotification(
                    joinReq.getUserId(), me.getId(), "group_join_request",
                    "GROUP", groupId,
                    "Permintaan bergabungmu ke grup \"" + (group != null ? group.getName() : "") + "\" disetujui!",
                    "{}");
        }
        return new DataResponse<>(SUCCESS, "Request " + request.getAction() + "d", HttpStatus.OK.value(), null);
    }

    private String generateSlug(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}