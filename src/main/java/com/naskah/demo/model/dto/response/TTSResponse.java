package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TTSResponse {
    private String audioUrl;
    private String text;
    private String voice;
    private Double duration; // in seconds
    private String format = "mp3";
    private Long fileSize;
    private LocalDateTime generatedAt;
    private String expiresAt;
}
