package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserContributionStats {
    private Integer pagesTranslated;
    private Integer pagesEdited;
    private Integer pagesIllustrated;
    private Integer pagesProofread;
    private Integer pagesTranscribed;
    private Integer pagesReviewed;
    private Integer totalContributions;

    private Integer projectsCreated;
    private Integer projectsJoined;
    private Integer projectsFollowed;

    private BigDecimal averageQualityScore;
    private Long totalReactionsReceived;
    private Long totalCommentsReceived;
}