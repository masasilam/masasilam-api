package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookReviewReplyResponse {
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
