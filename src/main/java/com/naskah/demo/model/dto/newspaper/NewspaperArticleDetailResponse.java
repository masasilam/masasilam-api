package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperArticleDetailResponse {
    private Long id;
    private String slug;

    // Source
    private NewspaperSourceResponse source;

    // Category & Date
    private String category;
    private String categoryName;
    private LocalDate publishDate;
    private String dateFormatted;

    // Content
    private String title;
    private String subtitle;
    private String bodyOriginal;
    private String bodyModern;
    private String excerpt;

    // Metadata
    private String author;
    private Integer pageNumber;
    private Integer columnNumber;
    private String importance;

    // Statistics
    private Long viewCount;
    private Long readCount;
    private Integer shareCount;
    private Integer saveCount;
    private Integer commentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;

    // User-specific data
    private Boolean isSaved;
    private Double myRating;
    private Boolean hasReviewed;

    // Related content
    private List<NewspaperArticleResponse> relatedArticles;
    private List<NewspaperArticleResponse> sameDateArticles;

    // Tags
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
