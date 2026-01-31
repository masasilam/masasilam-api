package com.naskah.demo.model.entity.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReadingHistory {
    private Long id;
    private Long userId;
    private Long articleId;
    private String category;
    private LocalDate publishDate;
    private Integer readingTimeSeconds;
    private Boolean completed;
    private LocalDateTime createdAt;
}
