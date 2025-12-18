package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EndReadingRequest {
    @NotNull(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "Chapter number is required")
    private Integer chapterNumber;

    private Integer endPosition;
    private Double scrollDepthPercentage;
    private Integer wordsRead;
    private Integer interactionCount;
}