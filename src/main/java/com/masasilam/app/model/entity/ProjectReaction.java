package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.ReactionType;
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
