package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterAudio {
    private Long id;
    private Long bookId;
    private Integer chapterNumber;
    private String audioUrl;
    private Long duration; // seconds
    private Long fileSize; // bytes
    private String format; // mp3, m4a, ogg
    private String voice; // Voice name/ID used
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}