package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class GoalsResponse {
    private GoalsSummary       summary;
    private List<Object>       active;
    private List<Object>       completed;
}