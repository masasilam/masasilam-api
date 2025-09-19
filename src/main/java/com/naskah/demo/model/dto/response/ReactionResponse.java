package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReactionResponse {
    private Long id;
    private String reactionType;
    private Integer page;
    private String position;
    private String userName;
    private LocalDateTime createdAt;
    private ReactionStats stats;

    @Data
    public static class ReactionStats {
        private Long totalLikes;
        private Long totalLoves;
        private Long totalStars;
        private Boolean userHasReacted;
        private String userReactionType;
    }
}