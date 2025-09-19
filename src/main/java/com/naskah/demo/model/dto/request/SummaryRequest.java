package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SummaryRequest {
    @NotNull
    @Min(1)
    private Integer chapter;

    private String summaryType = "BRIEF"; // BRIEF, DETAILED, BULLET_POINTS
    private Integer maxLength = 200; // words
}