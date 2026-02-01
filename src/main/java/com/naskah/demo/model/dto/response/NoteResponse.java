package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoteResponse {
    private Long id;
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