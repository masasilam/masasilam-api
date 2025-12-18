package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartReadingRequest {
    @NotNull(message = "Chapter number is required")
    private Integer chapterNumber;

    private String sessionId; // Client-generated UUID
    private Integer startPosition;
    private String deviceType; // mobile, tablet, desktop
    private String source; // web, app
}

