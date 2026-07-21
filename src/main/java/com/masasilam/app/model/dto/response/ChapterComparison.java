package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
class ChapterComparison {
    private String metric;
    private Double currentValue;
    private Double comparisonValue;
    private Double percentageDifference;
    private String trend;
}
