package com.masasilam.app.model.entity.newspaper;

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
public class NewspaperArticle {
    private Long id;
    private Long sourceId;
    private String slug;
    private String category;
    private LocalDate publishDate;
    private String title;
    private String subtitle;
    private String content;
    private String htmlContent;
    private Integer wordCount;
    private String author;
    private Integer pageNumber;
    private String importance;
    private Long parentArticleId;
    private Integer articleLevel;
    private BigDecimal averageRating;
    private Integer totalRatings;
    private String ratingDistribution;
    private Long viewCount;
    private Integer saveCount;
    private Integer commentCount;
    private String imageUrl;
    private String searchVector;
    private Boolean isActive;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}