package com.masasilam.app.model.dto.request;

import lombok.Data;

@Data
public class EpubStartReadingRequest {
    private String sessionId;
    private String deviceType;
    private String source;
    private String chapterLabel;
    private Integer chapterIndex;
    private Integer totalChapters;
}