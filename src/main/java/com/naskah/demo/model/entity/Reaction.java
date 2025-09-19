package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Reaction {
    private Long id;
    private Long userId;
    private Long bookId;
    private String reactionType; // LIKE, LOVE, STAR, THUMB_UP, etc.
    private Integer page;
    private String position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}