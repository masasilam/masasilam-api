package com.masasilam.app.model.entity.film;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmReviewReply {
    private Long id;
    private Long reviewId;
    private Long userId;
    private Long parentReplyId;
    private String content;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}