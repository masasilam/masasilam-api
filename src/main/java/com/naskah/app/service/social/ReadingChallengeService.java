package com.naskah.app.service.social;

import com.naskah.app.model.dto.request.social.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;

public interface ReadingChallengeService {
    DataResponse<ReadingChallengeResponse> createChallenge(CreateChallengeRequest request);
    DataResponse<ReadingChallengeResponse> updateChallenge(Long challengeId, CreateChallengeRequest request);
    DataResponse<Void> deleteChallenge(Long challengeId);
    DataResponse<ReadingChallengeResponse> getChallengeDetail(Long challengeId);
    DataResponse<ReadingChallengeResponse> getChallengeBySlug(String slug);
    DatatableResponse<ReadingChallengeResponse> getActiveChallenges(int page, int limit);
    DatatableResponse<ReadingChallengeResponse> getMyChallenges(String status, int page, int limit);
    DataResponse<Void> joinChallenge(Long challengeId);
    DataResponse<Void> abandonChallenge(Long challengeId);
    DataResponse<ReadingChallengeResponse> recordProgress(Long challengeId, String entityType, Long entityId);
    DataResponse<ChallengeLeaderboardResponse> getLeaderboard(Long challengeId, int page, int limit);
    // Called internally when reading progress hits 95%+
    void checkAndUpdateChallenges(Long userId, String entityType, Long entityId,
                                  String entityTitle, String entitySlug, String entityCover);
}