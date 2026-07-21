package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoiceNoteResponse {
    private Long id;
    private Long bookId;
    private Integer page;
    private String position;
    private String audioUrl;
    private Double duration;
    private String transcription;
    private LocalDateTime createdAt;
}