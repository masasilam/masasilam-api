package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TranslatedHighlightResponse {
    private Long highlightId;
    private String originalText;
    private String translatedText;
    private String targetLanguage;
    private LocalDateTime translatedAt;
    private Boolean isSaved; // Whether translation is saved for future use
}