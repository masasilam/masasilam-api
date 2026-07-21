package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class TrendAnalysis {
    private String readersGrowth;
    private String engagementTrend;
    private String popularityTrend;
    private Integer readersChangePercentage;
    private Integer engagementChangePercentage;
    private Integer estimatedReadersNextMonth;
    private Double estimatedCompletionRate;
}