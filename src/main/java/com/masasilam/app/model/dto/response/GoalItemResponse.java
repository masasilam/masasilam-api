package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GoalItemResponse {
    private Long id;
    private String title;
    private String description;
    private String type;
    private Integer target;
    private Integer current;
    private String unit;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysRemaining;
    private LocalDateTime completedAt;
    private String status;
}