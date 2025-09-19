package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HighlightRequest {
    @NotNull
    @Min(1)
    private Integer page;

    @NotNull
    private String startPosition;

    @NotNull
    private String endPosition;

    @NotBlank
    private String highlightedText;

    private String color = "#FFFF00"; // Default yellow
    private String note;
}