package com.naskah.app.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoteComment {
    private Long id;
    private Long noteId;
    private Long userId;
    private String content;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}