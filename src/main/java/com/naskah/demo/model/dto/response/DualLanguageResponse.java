package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class DualLanguageResponse {
    private Integer page;
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private Boolean isTranslationCached;
}