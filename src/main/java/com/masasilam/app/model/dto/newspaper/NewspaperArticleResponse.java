package com.masasilam.app.model.dto.newspaper;

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
    private Long sourceId;
    private String sourceName;
    private String sourceSlug;
    private String sourceLocation;
    private LocalDate publishDate;
    private String dateFormatted;
    private String title;
    private String subtitle;
    private String excerpt;
    private String authorNames;
    private String authorSlugs;
    private String authorPhotoUrls;
    private String genres;
    private String genreSlugs;
    private String contributors;
    private Integer pageNumber;
    private String importance;
    private Long viewCount;
    private Integer saveCount;
    private Integer commentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Boolean isSaved;
    private Double myRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}