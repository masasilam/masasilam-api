package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class MonthlyStatsResponse {
    private String month; // Format: YYYY-MM
    private Long posts;
    private Long views;
    private Long likes;
    private Long comments;
}
