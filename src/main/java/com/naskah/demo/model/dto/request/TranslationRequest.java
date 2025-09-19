package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationRequest {
    @NotBlank
    private String text;

    @NotBlank
    private String targetLanguage;

    private String sourceLanguage; // Auto-detect if null
}