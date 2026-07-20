package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.ReactionType;
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