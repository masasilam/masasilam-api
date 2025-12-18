package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingSessionResponse {
    private String sessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalDurationSeconds;
    private Integer chaptersRead;
    private List<Integer> chaptersVisited;
    private Integer totalInteractions;
}
