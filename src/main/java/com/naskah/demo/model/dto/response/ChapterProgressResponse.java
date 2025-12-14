package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterProgressResponse {
    private Integer chapterNumber;
    private Integer position;
    private Integer readingTimeSeconds;
    private Boolean isCompleted;
    private LocalDateTime lastReadAt;
}
