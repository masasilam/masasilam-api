package com.masasilam.app.model.dto.response;

import com.masasilam.app.model.enums.DifficultyLevel;
import com.masasilam.app.model.enums.ProjectPriority;
import com.masasilam.app.model.enums.ProjectStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectResponse {
    private Long id;
    private String title;
    private String description;
    private String author;
    private String originalSource;
    private DifficultyLevel difficulty;
    private ProjectStatus status;
    private ProjectPriority priority;
    private String originalLanguage;
    private String genre;
    private String originalPublicationYear;
    private Boolean isPublicDomain;
    private Integer totalPages;
    private Integer pagesCompleted;
    private BigDecimal overallProgress;
    private Map<String, Integer> progressDetails;
    private LocalDate startDate;
    private LocalDateTime expectedCompletionDate;
    private LocalDate actualCompletionDate;
    private String coverImageUrl;
    private String bookSlug;
    private Integer contributorCount;
    private Integer followerCount;
    private Integer commentCount;
    private BigDecimal qualityScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userReaction;
    private List<ReactionSummary> reactionBreakdown;
    private Boolean isFollowing;
    private List<ProjectCommentResponse> recentComments;
}