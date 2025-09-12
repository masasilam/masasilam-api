package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectComment {
    private Long id;
    private Long projectId;
    private Long userId;
    private String content;
    private Long parentCommentId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Long reactionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}