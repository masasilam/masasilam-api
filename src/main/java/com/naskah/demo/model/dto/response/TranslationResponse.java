package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class TranslationResponse {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private Double confidenceScore;
    private String translationProvider = "Microsoft Translator";
}