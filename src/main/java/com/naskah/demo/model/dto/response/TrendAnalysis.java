package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class TrendAnalysis {
    private String readersGrowth; // "increasing", "stable", "decreasing"
    private String engagementTrend; // "improving", "stable", "declining"
    private String popularityTrend; // "rising", "stable", "falling"

    // Week over week
    private Integer readersChangePercentage;
    private Integer engagementChangePercentage;

    // Predictions
    private Integer estimatedReadersNextMonth;
    private Double estimatedCompletionRate;
}