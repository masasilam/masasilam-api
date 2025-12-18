package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingHeartbeatRequest {
    @NotNull(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "Chapter number is required")
    private Integer chapterNumber;

    @NotNull(message = "Current position is required")
    private Integer currentPosition;

    private Double scrollDepth;
    private Integer interactionCount;
}
