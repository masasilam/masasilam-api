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
    private String position;
    private String content;
    private String selectedText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}