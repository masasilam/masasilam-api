package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.DifficultyLevel;
import com.naskah.demo.model.enums.ProjectPriority;
import com.naskah.demo.model.enums.ProjectStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Project {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String author;
    private String originalSource;
    private DifficultyLevel difficulty;
    private ProjectStatus status;
    private ProjectPriority priority;
    private Integer originalLanguage;
    private String originalTitle;
    private Integer estimatedPages;
    private Integer estimatedWordCount;
    private String originalPublicationYear;
    private Boolean isPublicDomain;
    private String copyrightStatus;
    private String metadataJson;
    private Integer totalPages;
    private Integer transcriptionCompletedPages;
    private Integer translationCompletedPages;
    private Integer editingCompletedPages;
    private Integer illustrationCompletedPages;
    private Integer proofreadingCompletedPages;
    private Integer formattedPages;
    private Integer pagesCompleted;
    private BigDecimal overallProgress;
    private LocalDateTime expectedCompletionDate;
    private LocalDate startDate;
    private LocalDate actualCompletionDate;
    private BigDecimal qualityScore;
    private Integer issuesReported;
    private Integer issuesResolved;
    private String bookSlug;
    private String coverImageUrl;
    private String sourceFileUrl;
    private String epubFilePath;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private Long createdBy;
    private Long viewCount;
    private Long followerCount;
    private Long reactionCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
