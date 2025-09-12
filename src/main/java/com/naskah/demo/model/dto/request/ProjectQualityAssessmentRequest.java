package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectQualityAssessmentRequest {
    @NotNull(message = "Quality score is required")
    private Integer qualityScore;

    @NotNull(message = "Content accuracy rating is required")
    private Integer contentAccuracy;

    @NotNull(message = "Language quality rating is required")
    private Integer languageQuality;

    @NotNull(message = "Formatting quality rating is required")
    private Integer formattingQuality;

    @NotNull(message = "Overall readability rating is required")
    private Integer overallReadability;

    private String recommendations;
}