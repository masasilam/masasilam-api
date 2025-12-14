package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterTextResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private String plainText;
    private Integer wordCount;
    private Integer estimatedDuration; // in seconds
}