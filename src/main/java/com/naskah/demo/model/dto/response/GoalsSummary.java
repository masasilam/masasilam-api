package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class GoalsSummary {
    private Integer total;
    private Integer completed;
    private Integer active;
    private Integer thisMonth;
}