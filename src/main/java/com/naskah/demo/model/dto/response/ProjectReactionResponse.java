package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectReactionResponse {
    private String reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
