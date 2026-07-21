package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class ChapterDropOffPoint {
    private Integer chapterNumber;
    private String chapterTitle;
    private Double dropOffRate;
    private Integer averageScrollDepth;
    private Integer readersStarted;
    private Integer readersCompleted;
    private String severity;
}