package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterEngagement {
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer annotationCount;
    private Double engagementScore;
    private Integer uniqueReaders;
}
