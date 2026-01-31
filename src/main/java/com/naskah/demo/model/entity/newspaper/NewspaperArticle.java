package com.naskah.demo.model.entity.newspaper;

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
    // Primary Key
    private Long id;

    // Relationship
    private Long sourceId; // FK to newspaper_sources

    // Indexing
    private String slug;
    private String category; // olahraga, politik, ekonomi, budaya
    private LocalDate publishDate;

    // Content
    private String title;
    private String content;
    private String htmlContent;
    private Integer wordCount;

    // Metadata
    private String author;
    private Integer pageNumber;
    private String importance; // high, medium, low

    // Hierarchical (for multi-part articles)
    private Long parentArticleId;
    private Integer articleLevel; // 0 = main, 1+ = sub-parts

    // Ratings
    private BigDecimal averageRating; // 0.00 - 5.00
    private Integer totalRatings;
    private String ratingDistribution; // JSON: {"5": 10, "4": 5, ...}

    // Statistics
    private Long viewCount;
    private Integer saveCount;
    private Integer commentCount;

    // Media
    private String imageUrl;

    // Search
    private String searchVector; // tsvector for full-text search

    // Publishing
    private Boolean isActive;
    private Boolean isFeatured;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}