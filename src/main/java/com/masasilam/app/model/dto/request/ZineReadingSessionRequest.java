package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZineReadingSessionRequest {
    @NotNull(message = "startedAt tidak boleh kosong")
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    @Min(value = 0, message = "Durasi tidak boleh negatif")
    private Integer totalDurationSeconds = 0;
    private Integer startChapter;
    private Integer endChapter;
    private Integer chaptersRead = 1;
    private String sessionType = "EPUB";
}