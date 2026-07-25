package com.masasilam.app.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContentSearchCriteria {
    private String contentType;
    private String searchTitle;
    private String searchInBook;
    private String authorName;
    private String contributor;
    private String genre;
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
    private Double minRating;
    private Integer minViewCount;
    private Integer minReadCount;
}