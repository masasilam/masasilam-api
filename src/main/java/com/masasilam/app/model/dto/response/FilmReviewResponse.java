package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FilmReviewResponse {
    private Long id;
    private Long filmId;
    private Long userId;
    private String username;
    private String userPhotoUrl;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean isOwner;
    private Boolean currentUserFeedback;
    private List<FilmReviewReplyResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}