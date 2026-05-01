package com.naskah.demo.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineReadingSession {
    private Long id;
    private Long userId;
    private Long zineId;
    private String sessionType; // selalu "EPUB" untuk zine
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalDurationSeconds;
    private Integer chaptersRead;
    private Integer startChapter;
    private Integer endChapter;
    private LocalDateTime createdAt;
}