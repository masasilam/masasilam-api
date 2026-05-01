package com.naskah.demo.service.social.impl;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.mapper.social.ReadingChallengeMapper;
import com.naskah.demo.model.dto.request.social.CreateChallengeRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.model.entity.social.*;
import com.naskah.demo.service.social.*;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingChallengeServiceImpl implements ReadingChallengeService {

    private final ReadingChallengeMapper challengeMapper;
    private final UserMapper userMapper;
    private final ActivityFeedService feedService;
    private final NotificationService notificationService;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

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
        } catch (Exception e) { return null; }
    }

    @Override
    @Transactional
    public DataResponse<ReadingChallengeResponse> createChallenge(CreateChallengeRequest request) {
        User me = requireAuth();
        String slug = generateSlug(request.getTitle());
        String finalSlug = slug;
        int c = 2;
        while (challengeMapper.findBySlug(finalSlug) != null) finalSlug = slug + "-" + c++;

        ReadingChallenge challenge = new ReadingChallenge();
        challenge.setCreatedBy(me.getId());
        challenge.setTitle(request.getTitle());
        challenge.setSlug(finalSlug);
        challenge.setDescription(request.getDescription());
        challenge.setCoverImageUrl(request.getCoverImageUrl());
        challenge.setChallengeType(request.getChallengeType());

        // PERBAIKAN: Kirim sebagai List, bukan String
        // StringArrayTypeHandler akan mengkonversi List ke PostgreSQL array
        challenge.setEntityTypes(request.getEntityTypes() != null && !request.getEntityTypes().isEmpty()
                ? request.getEntityTypes() : List.of("BOOK"));

        challenge.setTargetCount(request.getTargetCount());

        // PERBAIKAN: Kirim sebagai List, bukan String
        challenge.setRequiredGenres(request.getRequiredGenres() != null && !request.getRequiredGenres().isEmpty()
                ? request.getRequiredGenres() : null);

        challenge.setRequiredListId(request.getRequiredListId());
        challenge.setStartDate(request.getStartDate());
        challenge.setEndDate(request.getEndDate());
        challenge.setXpReward(request.getXpReward() != null ? request.getXpReward() : 0);
        challenge.setBadgeName(request.getBadgeName());
        challenge.setBadgeImageUrl(request.getBadgeImageUrl());
        challenge.setIsOfficial(false);
        challengeMapper.insertChallenge(challenge);

        ReadingChallengeResponse response =
                challengeMapper.getChallengeDetail(challenge.getId(), me.getId());
        return new DataResponse<>(SUCCESS, "Challenge created", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<ReadingChallengeResponse> updateChallenge(
            Long challengeId, CreateChallengeRequest request) {
        User me = requireAuth();
        ReadingChallenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) throw new DataNotFoundException();
        if (!challenge.getCreatedBy().equals(me.getId())) throw new ForbiddenException();

        if (request.getTitle() != null)       challenge.setTitle(request.getTitle());
        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getStartDate() != null)   challenge.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)     challenge.setEndDate(request.getEndDate());
        if (request.getXpReward() != null)    challenge.setXpReward(request.getXpReward());
        if (request.getBadgeName() != null)   challenge.setBadgeName(request.getBadgeName());
        if (request.getBadgeImageUrl() != null) challenge.setBadgeImageUrl(request.getBadgeImageUrl());
        challengeMapper.updateChallenge(challenge);

        ReadingChallengeResponse response =
                challengeMapper.getChallengeDetail(challengeId, me.getId());
        return new DataResponse<>(SUCCESS, "Challenge updated", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChallenge(Long challengeId) {
        User me = requireAuth();
        ReadingChallenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) throw new DataNotFoundException();
        if (!challenge.getCreatedBy().equals(me.getId())) throw new ForbiddenException();
        challengeMapper.softDeleteChallenge(challengeId);
        return new DataResponse<>(SUCCESS, "Challenge deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<ReadingChallengeResponse> getChallengeDetail(Long challengeId) {
        Long currentUserId = currentUserIdOrNull();
        ReadingChallengeResponse response =
                challengeMapper.getChallengeDetail(challengeId, currentUserId);
        if (response == null) throw new DataNotFoundException();
        if (currentUserId != null) {
            ChallengeParticipant participant =
                    challengeMapper.findParticipant(challengeId, currentUserId);
            if (participant != null) {
                List<ChallengeProgressItemResponse> items =
                        challengeMapper.findProgressByParticipant(participant.getId());
                response.setMyItems(items);
                if (response.getTargetCount() != null && response.getTargetCount() > 0) {
                    response.setMyProgressPercent(
                            (participant.getProgressCount() * 100.0) / response.getTargetCount());
                }
            }
        }
        return new DataResponse<>(SUCCESS, "Challenge retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<ReadingChallengeResponse> getChallengeBySlug(String slug) {
        ReadingChallenge challenge = challengeMapper.findBySlug(slug);
        if (challenge == null) throw new DataNotFoundException();
        return getChallengeDetail(challenge.getId());
    }

    @Override
    public DatatableResponse<ReadingChallengeResponse> getActiveChallenges(int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<ReadingChallengeResponse> challenges =
                challengeMapper.findActiveChallenges(currentUserId, offset, limit);
        int total = challengeMapper.countActiveChallenges();
        return new DatatableResponse<>(SUCCESS, "Challenges retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, challenges));
    }

    @Override
    public DatatableResponse<ReadingChallengeResponse> getMyChallenges(
            String status, int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<ReadingChallengeResponse> challenges =
                challengeMapper.findUserChallenges(me.getId(), status, offset, limit);
        int total = challengeMapper.countUserChallenges(me.getId(), status);
        return new DatatableResponse<>(SUCCESS, "My challenges retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, challenges));
    }

    @Override
    @Transactional
    public DataResponse<Void> joinChallenge(Long challengeId) {
        User me = requireAuth();
        ReadingChallenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null || !challenge.getIsActive()) throw new DataNotFoundException();

        ChallengeParticipant existing = challengeMapper.findParticipant(challengeId, me.getId());
        if (existing != null) throw new BadRequestException("Already joined this challenge");

        ChallengeParticipant participant = new ChallengeParticipant();
        participant.setChallengeId(challengeId);
        participant.setUserId(me.getId());
        challengeMapper.insertParticipant(participant);
        challengeMapper.incrementParticipantCount(challengeId);

        feedService.publishActivity(me.getId(), "joined_challenge", "CHALLENGE",
                challengeId, challenge.getSlug(), challenge.getTitle(),
                challenge.getCoverImageUrl(), "{}", "public");

        return new DataResponse<>(SUCCESS, "Joined challenge", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> abandonChallenge(Long challengeId) {
        User me = requireAuth();
        ChallengeParticipant participant = challengeMapper.findParticipant(challengeId, me.getId());
        if (participant == null) throw new DataNotFoundException();
        participant.setStatus("abandoned");
        challengeMapper.updateParticipant(participant);
        return new DataResponse<>(SUCCESS, "Challenge abandoned", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<ReadingChallengeResponse> recordProgress(
            Long challengeId, String entityType, Long entityId) {
        User me = requireAuth();
        ChallengeParticipant participant = challengeMapper.findParticipant(challengeId, me.getId());
        if (participant == null) throw new BadRequestException("Not participating in this challenge");
        if ("completed".equals(participant.getStatus()))
            throw new BadRequestException("Challenge already completed");

        boolean alreadyRecorded = challengeMapper.progressItemExists(
                participant.getId(), entityType, entityId);
        if (alreadyRecorded) throw new BadRequestException("Already recorded for this item");

        ChallengeProgressItem item = new ChallengeProgressItem();
        item.setParticipantId(participant.getId());
        item.setEntityType(entityType);
        item.setEntityId(entityId);
        challengeMapper.insertProgressItem(item);

        participant.setProgressCount(participant.getProgressCount() + 1);

        ReadingChallenge challenge = challengeMapper.findById(challengeId);
        boolean completed = challenge.getTargetCount() != null
                && participant.getProgressCount() >= challenge.getTargetCount();

        if (completed) {
            participant.setStatus("completed");
            participant.setCompletedAt(LocalDateTime.now());
            challengeMapper.incrementCompletionCount(challengeId);

            notificationService.sendNotification(me.getId(), null,
                    "challenge_completed", "CHALLENGE", challengeId,
                    "Selamat! Kamu menyelesaikan tantangan \"" + challenge.getTitle() + "\"! 🎉",
                    "{\"xpReward\":" + challenge.getXpReward() + "}");

            feedService.publishActivity(me.getId(), "completed_challenge", "CHALLENGE",
                    challengeId, challenge.getSlug(), challenge.getTitle(),
                    challenge.getCoverImageUrl(), "{}", "public");
        }

        challengeMapper.updateParticipant(participant);
        return getChallengeDetail(challengeId);
    }

    @Override
    public DataResponse<ChallengeLeaderboardResponse> getLeaderboard(
            Long challengeId, int page, int limit) {
        ReadingChallenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) throw new DataNotFoundException();
        int offset = (page - 1) * limit;
        List<ChallengeLeaderboardResponse.LeaderboardEntryResponse> entries =
                challengeMapper.getLeaderboard(challengeId, offset, limit);
        ChallengeLeaderboardResponse response = new ChallengeLeaderboardResponse();
        response.setChallengeId(challengeId);
        response.setChallengeTitle(challenge.getTitle());
        response.setEntries(entries);
        return new DataResponse<>(SUCCESS, "Leaderboard retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public void checkAndUpdateChallenges(Long userId, String entityType, Long entityId,
                                         String entityTitle, String entitySlug, String entityCover) {
        try {
            List<ReadingChallengeResponse> activeChallenges =
                    challengeMapper.findUserChallenges(userId, "in_progress", 0, 100);
            for (ReadingChallengeResponse ch : activeChallenges) {
                ReadingChallenge challenge = challengeMapper.findById(ch.getId());
                if (challenge == null) continue;
                String types = challenge.getEntityTypes() != null ? String.valueOf(challenge.getEntityTypes()) : "BOOK";
                if (!types.contains(entityType)) continue;

                ChallengeParticipant participant =
                        challengeMapper.findParticipant(ch.getId(), userId);
                if (participant == null) continue;

                boolean recorded = challengeMapper.progressItemExists(
                        participant.getId(), entityType, entityId);
                if (recorded) continue;

                ChallengeProgressItem item = new ChallengeProgressItem();
                item.setParticipantId(participant.getId());
                item.setEntityType(entityType);
                item.setEntityId(entityId);
                item.setEntityTitle(entityTitle);
                item.setEntitySlug(entitySlug);
                item.setEntityCover(entityCover);
                challengeMapper.insertProgressItem(item);

                participant.setProgressCount(participant.getProgressCount() + 1);
                boolean completed = challenge.getTargetCount() != null
                        && participant.getProgressCount() >= challenge.getTargetCount();
                if (completed) {
                    participant.setStatus("completed");
                    participant.setCompletedAt(LocalDateTime.now());
                    challengeMapper.incrementCompletionCount(ch.getId());
                    notificationService.sendNotification(userId, null,
                            "challenge_completed", "CHALLENGE", ch.getId(),
                            "Selamat! Kamu menyelesaikan tantangan \"" + challenge.getTitle() + "\"! 🎉",
                            "{\"xpReward\":" + challenge.getXpReward() + "}");
                }
                challengeMapper.updateParticipant(participant);
            }
        } catch (Exception e) {
            log.warn("checkAndUpdateChallenges failed for user={}: {}", userId, e.getMessage());
        }
    }

    private String generateSlug(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}