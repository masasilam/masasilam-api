package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterSkipAnalysis {
    private Integer chapterNumber;
    private String chapterTitle;
    private Double skipRate;
    private Integer timesSkipped;
    private Integer totalReaders;
    private String possibleReason; // "Too long", "Low rating", "Complex"
}