package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterAudioResponse {
    private Long id;
    private Long bookId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String audioUrl;
    private Long duration; // seconds
    private Long fileSize; // bytes
    private String format;
}
