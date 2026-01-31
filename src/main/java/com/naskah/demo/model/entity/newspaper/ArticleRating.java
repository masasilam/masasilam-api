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
public class ArticleRating {
    private Long id;
    private Long userId;
    private Long articleId;
    private Double rating; // 1-5
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
