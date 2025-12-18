package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingProgressSummary {
    private Integer currentChapter;
    private Integer totalChapters;
    private Double completionPercentage;
    private LocalDateTime lastReadAt;
    private Integer totalReadingTimeMinutes;
    private List<Integer> completedChapters;
    private Integer chaptersInProgress;
}
