package com.naskah.demo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineSearchCriteria {
    private String searchTitle;
    private String searchInZine;
    private String authorName;
    private String contributor;
    private String genre;
    private Integer volume;
    private String issueNumber;
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