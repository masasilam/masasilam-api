package com.naskah.demo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookSearchCriteria {
    // Basic search
    private String searchTitle;
    private String searchInBook;
    private String authorName;
    private String contributor;
    private String genre;

    // Advanced filters
    private Integer minPages;
    private Integer maxPages;
    private Long minFileSize;
    private Long maxFileSize;
    private Integer publicationYearFrom;
    private Integer publicationYearTo;
    private String difficultyLevel;
    private String fileFormat;
    private Boolean isFeatured;
    private Integer languageId;

    // Rating and popularity
    private Double minRating;
    private Integer minViewCount;
    private Integer minReadCount;
}
