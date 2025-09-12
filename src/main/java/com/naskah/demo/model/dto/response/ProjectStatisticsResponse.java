package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectStatisticsResponse {
    private Long projectId;
    private Integer totalPages;
    private Integer completedPages;
    private BigDecimal overallProgress;
    private BigDecimal transcriptionProgress;
    private BigDecimal translationProgress;
    private BigDecimal editingProgress;
    private BigDecimal illustrationProgress;
    private BigDecimal proofreadingProgress;
    private Integer totalMembers;
    private Long totalFollowers;
    private Long totalReactions;
    private Long totalComments;
    private Long viewCount;
    private List<ReactionSummary> reactionBreakdown;
    private List<MemberRoleSummary> memberBreakdown;
    private List<ProjectActivityResponse> recentActivity;
}