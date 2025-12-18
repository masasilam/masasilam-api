package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Note {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String position;
    private String title;
    private String content;
    private String color;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}