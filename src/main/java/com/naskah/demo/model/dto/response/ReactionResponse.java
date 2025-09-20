package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReactionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userDisplayName;
    private Long bookId;
    private String reactionType;
    private Integer rating;
    private String comment;
    private String title; // Title untuk comment
    private Integer page;
    private String position;
    private Long parentId; // Untuk threading
    private Integer replyCount; // Jumlah reply
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Reaction stats
    private Long totalRatings;
    private Long totalAngry;
    private Long totalLikes;
    private Long totalLoves;
    private Long totalDislikes;  // Fixed: was missing
    private Long totalSad;       // Fixed: was missing
    private Long totalComments;  // Fixed: removed duplicate toTalComments (typo)
    private Double averageRating;
    private Boolean userHasReacted;
    private String userReactionType;
}