package com.masasilam.app.model.dto.newspaper;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private NewspaperSourceResponse source;
    @JsonIgnore
    private Long sourceId;
    @JsonIgnore
    private String sourceName;
    @JsonIgnore
    private String sourceLocation;
    @JsonIgnore
    private String sourceDescription;
    private String category;
    private String categoryName;
    private LocalDate publishDate;
    private String dateFormatted;
    private String title;
    private String subtitle;
    private String bodyOriginal;
    private String bodyModern;
    private String excerpt;
    private String author;
    private Integer pageNumber;
    private Integer columnNumber;
    private String importance;
    private Integer wordCount;
    private String imageUrl;
    private Long parentArticleId;
    private Integer articleLevel;
    private Long viewCount;
    private Long readCount;
    private Integer shareCount;
    private Integer saveCount;
    private Integer commentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Boolean isSaved;
    private Double myRating;
    private Boolean hasReviewed;
    private List<NewspaperArticleResponse> relatedArticles;
    private List<NewspaperArticleResponse> sameDateArticles;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}