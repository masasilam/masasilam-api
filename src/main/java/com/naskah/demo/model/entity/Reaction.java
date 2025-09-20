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
    private Integer page;
    private String position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}