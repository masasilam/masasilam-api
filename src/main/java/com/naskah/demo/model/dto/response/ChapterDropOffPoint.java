package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterDropOffPoint {
    private Integer chapterNumber;
    private String chapterTitle;
    private Double dropOffRate; // % of readers who don't continue
    private Integer averageScrollDepth;
    private Integer readersStarted;
    private Integer readersCompleted;
    private String severity; // "Critical", "High", "Medium", "Low"
}