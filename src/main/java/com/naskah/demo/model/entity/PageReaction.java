package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.ReactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageReaction {
    private Long id;
    private Long pageId;
    private Long userId;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}