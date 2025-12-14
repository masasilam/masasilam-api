package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookChapter {
    private Long id;
    private Long bookId;
    private Integer chapterNumber;
    private Long parentChapterId;
    private Integer chapterLevel;
    private String title;
    private String slug;
    private String content;
    private String htmlContent;
    private Integer wordCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}