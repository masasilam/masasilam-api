package com.naskah.demo.model.dto.response;

import com.naskah.demo.model.enums.DifficultyLevel;
import com.naskah.demo.model.enums.ProjectPriority;
import com.naskah.demo.model.enums.ProjectStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectResponse {
    // Basic info
    private Long id;
    private String title;
    private String description;
    private String author;
    private String originalSource;

    // Project metadata
    private DifficultyLevel difficulty;
    private ProjectStatus status;
    private ProjectPriority priority;
    private String originalLanguage;
    private String genre;
    private String originalPublicationYear;
    private Boolean isPublicDomain;

    // Progress tracking
    private Integer totalPages;
    private Integer pagesCompleted;
    private BigDecimal overallProgress;
    private Map<String, Integer> progressDetails;

    // Timeline
    private LocalDate startDate;
    private LocalDateTime expectedCompletionDate;
    private LocalDate actualCompletionDate;

    // Media & URLs
    private String coverImageUrl;
    private String bookSlug;

    // Community metrics
    private Integer contributorCount;
    private Integer followerCount;
    private Integer commentCount;

    // Quality metrics
    private BigDecimal qualityScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String userReaction;

    private List<ReactionSummary> reactionBreakdown;

    private Boolean isFollowing;

    private List<ProjectCommentResponse> recentComments;
}