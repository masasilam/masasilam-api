package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
class ChapterComparison {
    private String metric; // e.g., "completion_rate"
    private Double currentValue;
    private Double comparisonValue;
    private Double percentageDifference;
    private String trend; // "better", "worse", "similar"
}
