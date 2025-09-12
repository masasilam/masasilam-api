package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.CommentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageComment {
    private Long id;
    private Long pageId;
    private Long userId;
    private String content;
    private CommentType commentType;
    private Long parentCommentId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Long reactionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}