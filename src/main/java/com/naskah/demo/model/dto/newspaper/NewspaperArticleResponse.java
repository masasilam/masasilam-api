package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperArticleResponse {
    private Long id;
    private String slug;

    // Source
    private Long sourceId;
    private String sourceName;
    private String sourceLocation;

    // Category & Date
    private String category;
    private String categoryName;
    private LocalDate publishDate;
    private String dateFormatted;

    // Content
    private String title;
    private String subtitle;
    private String excerpt;

    // Metadata
    private String author;
    private Integer pageNumber;
    private String importance;

    // Statistics
    private Long viewCount;
    private Integer saveCount;
    private Integer commentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;

    // User-specific (if authenticated)
    private Boolean isSaved;
    private Double myRating;

    private LocalDateTime createdAt;
}