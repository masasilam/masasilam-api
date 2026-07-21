package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class MonthlyStatsResponse {
    private String month;
    private Long posts;
    private Long views;
    private Long likes;
    private Long comments;
}
