package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterStatsResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer readerCount;
    private Double completionRate;
    private Integer averageReadingTimeMinutes;
    private Integer highlightCount;
    private Integer noteCount;
    private Integer commentCount;
}
