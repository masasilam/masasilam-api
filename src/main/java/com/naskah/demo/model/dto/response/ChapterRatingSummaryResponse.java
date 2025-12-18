package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterRatingSummaryResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private Double averageRating;
    private Integer totalRatings;
    private RatingDistribution distribution;
    private Integer myRating; // Current user's rating
}
