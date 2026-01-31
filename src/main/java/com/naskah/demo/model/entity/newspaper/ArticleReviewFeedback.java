package com.naskah.demo.model.entity.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReviewFeedback {
    private Long id;
    private Long userId;
    private Long reviewId;
    private Boolean isHelpful;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
