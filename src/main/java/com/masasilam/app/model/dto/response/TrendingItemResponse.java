package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingItemResponse {
    private String contentType;
    private Long contentId;
    private String slug;
    private String title;
    private String subtitle;
    private String coverImageUrl;
    private String posterPortraitUrl;
    private String authorNames;
    private String releaseYear;
    private String description;
    private String sourceSlug;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer uniqueViewers;
    private Double score;
    private Double normalizedScore;
    private Integer rankPosition;
    private LocalDateTime computedAt;
}