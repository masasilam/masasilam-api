package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GroupReadingSchedule {
    private Long id;
    private Long groupId;
    private String entityType;
    private Long entityId;
    private String entityTitle;
    private String entitySlug;
    private Integer chapterStart;
    private Integer chapterEnd;
    private String chapterLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private String discussionPrompt;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
}