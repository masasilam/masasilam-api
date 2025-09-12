package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.ReactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectReaction {
    private Long id;
    private Long projectId;
    private Long userId;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
