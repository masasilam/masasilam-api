package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Note {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private String selectedText;
    private String content;
    private Integer startPosition;
    private Integer endPosition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}