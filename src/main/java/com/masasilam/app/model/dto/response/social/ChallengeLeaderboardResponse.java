package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChallengeLeaderboardResponse {
    private Long challengeId;
    private String challengeTitle;
    private List<LeaderboardEntryResponse> entries;

    @Data
    public static class LeaderboardEntryResponse {
        private Integer rank;
        private Long userId;
        private String username;
        private String displayName;
        private String userPhoto;
        private Integer progressCount;
        private String status;
        private LocalDateTime completedAt;
        private LocalDateTime joinedAt;
    }
}