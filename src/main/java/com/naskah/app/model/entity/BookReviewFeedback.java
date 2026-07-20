package com.naskah.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookReviewFeedback {
    private Long id;
    private Long userId;
    private Long reviewId;
    private Boolean isHelpful;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
