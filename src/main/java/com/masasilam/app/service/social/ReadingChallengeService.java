package com.masasilam.app.service.social;

import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;

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
}