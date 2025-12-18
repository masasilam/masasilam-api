package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingActivitySummary {
    private Integer chapterNumber;
    private String chapterTitle;
    private LocalDateTime lastReadAt;
    private Integer totalReadingTimeSeconds;
    private Integer timesRead;
    private Boolean isCompleted;
    private Double averageScrollDepth;
}
