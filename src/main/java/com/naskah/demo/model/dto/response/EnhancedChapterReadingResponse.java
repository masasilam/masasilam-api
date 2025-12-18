package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EnhancedChapterReadingResponse extends ChapterReadingResponse {
    // Add to existing ChapterReadingResponse

    // Rating info
    private ChapterRatingSummaryResponse ratingInfo;

    // Reading history
    private Integer timesRead;
    private LocalDateTime lastReadAt;
    private Integer totalReadingTimeSeconds;

    // Patterns
    private Boolean isFrequentlySkipped; // If skip rate > 30%
    private Boolean isPopular; // If read by many users
    private Integer currentReaders; // Active readers now
}