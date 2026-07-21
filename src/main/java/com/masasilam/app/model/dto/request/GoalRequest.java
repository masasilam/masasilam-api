package com.masasilam.app.model.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GoalRequest {
    private String title;
    private String description;
    private String goalType;
    private Integer targetValue;
    private String unit;
    private LocalDate startDate;
    private LocalDate endDate;
}
