package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Reaction {
    private Long id;
    private Long userId;
    private Long bookId;
    private String reactionType;
    private Integer rating;
    private String comment;
    private String title;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}