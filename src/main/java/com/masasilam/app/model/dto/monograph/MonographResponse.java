package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MonographResponse {
    private Long id;
    private String slug;
    private String title;
    private String subtitle;
    private String newspaperSourceName;
    private String newspaperSourceSlug;
    private LocalDate editionDate;
    private String editionDateFormatted;
    private String coverImageUrl;
    private String status;
    private Integer totalArticles;
    private Integer inBookCount;
    private Integer appendixCount;
    private Integer wordCount;
    private Integer readingTimeMinutes;
    private Integer viewCount;
    private LocalDateTime publishedAt;
}