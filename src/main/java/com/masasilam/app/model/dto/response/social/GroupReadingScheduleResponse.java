package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GroupReadingScheduleResponse {
    private Long id;
    private Long groupId;
    private String entityType;
    private Long entityId;
    private String entityTitle;
    private String entitySlug;
    private String entityCover;
    private Integer chapterStart;
    private Integer chapterEnd;
    private String chapterLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private String discussionPrompt;
    private Boolean isCompleted;
    private Integer discussionCount;
    private LocalDateTime createdAt;
}