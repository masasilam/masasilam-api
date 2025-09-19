package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoiceNote {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer page;
    private String position;
    private String audioUrl;
    private Double duration; // in seconds
    private String transcription;
    private LocalDateTime createdAt;
}