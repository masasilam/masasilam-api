package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TranslateHighlightRequest {
    @NotNull
    private Long highlightId;

    @NotBlank
    private String targetLanguage;
}