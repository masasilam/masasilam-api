package com.naskah.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZineReviewReplyResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long reviewId;
    private Long parentReplyId;
    private String content;
    private Boolean isOwner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}