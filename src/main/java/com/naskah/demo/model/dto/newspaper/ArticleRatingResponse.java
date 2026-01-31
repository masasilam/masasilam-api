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
public class ArticleRatingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long articleId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
