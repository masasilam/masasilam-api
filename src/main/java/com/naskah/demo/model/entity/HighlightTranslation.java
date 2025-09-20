package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HighlightTranslation {
    private Long id;
    private Long highlightId;
    private String targetLanguage;
    private String translatedText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}