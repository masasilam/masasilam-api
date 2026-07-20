package com.naskah.app.model.dto.response;

import lombok.Data;

@Data
public class GoalsSummary {
    private Integer total;
    private Integer completed;
    private Integer active;
    private Integer thisMonth;
}