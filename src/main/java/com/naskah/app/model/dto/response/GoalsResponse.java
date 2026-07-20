package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class GoalsResponse {
    private GoalsSummary           summary;
    private List<GoalItemResponse> active;
    private List<GoalItemResponse> completed;
}