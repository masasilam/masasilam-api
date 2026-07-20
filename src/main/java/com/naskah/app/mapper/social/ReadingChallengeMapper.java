package com.naskah.app.mapper.social;

import com.naskah.app.model.entity.social.*;
import com.naskah.app.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReadingChallengeMapper {
    void insertChallenge(ReadingChallenge challenge);
    void updateChallenge(ReadingChallenge challenge);
    void softDeleteChallenge(Long id);
    ReadingChallenge findById(Long id);
    ReadingChallenge findBySlug(String slug);
    ReadingChallengeResponse getChallengeDetail(@Param("challengeId") Long challengeId,
                                                @Param("currentUserId") Long currentUserId);

    List<ReadingChallengeResponse> findActiveChallenges(@Param("currentUserId") Long currentUserId,
                                                        @Param("offset") int offset, @Param("limit") int limit);
    List<ReadingChallengeResponse> findUserChallenges(@Param("userId") Long userId,
                                                      @Param("status") String status,
                                                      @Param("offset") int offset, @Param("limit") int limit);
    int countActiveChallenges();
    int countUserChallenges(@Param("userId") Long userId, @Param("status") String status);

    // Participants
    void insertParticipant(ChallengeParticipant participant);
    void updateParticipant(ChallengeParticipant participant);
    ChallengeParticipant findParticipant(@Param("challengeId") Long challengeId, @Param("userId") Long userId);
    void incrementParticipantCount(Long challengeId);
    void incrementCompletionCount(Long challengeId);

    // Progress
    void insertProgressItem(ChallengeProgressItem item);
    boolean progressItemExists(@Param("participantId") Long participantId,
                               @Param("entityType") String entityType, @Param("entityId") Long entityId);
    List<ChallengeProgressItemResponse> findProgressByParticipant(Long participantId);

    // Leaderboard
    List<ChallengeLeaderboardResponse.LeaderboardEntryResponse> getLeaderboard(@Param("challengeId") Long challengeId,
                                                                               @Param("offset") int offset,
                                                                               @Param("limit") int limit);
    int countLeaderboard(Long challengeId);
}