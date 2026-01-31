package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReviewReplyResponse {
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
