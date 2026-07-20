package com.naskah.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZineReadingSessionResponse {
    private Long    id;
    private Long    zineId;
    private String  zineSlug;
    private String  zineTitle;
    private String  sessionType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalDurationSeconds;
    private Integer chaptersRead;
    private Integer startChapter;
    private Integer endChapter;
    private LocalDateTime createdAt;
}