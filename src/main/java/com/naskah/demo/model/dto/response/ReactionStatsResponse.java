package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ReactionStatsResponse {
    private Long totalRatings;
    private Long totalAngry;
    private Long totalLikes;
    private Long totalLoves;
    private Long totalDislikes;
    private Long totalSad;
    private Long totalComments; // Total comment/diskusi
    private Double averageRating;
    private Boolean userHasReacted;
    private String userReactionType;
}