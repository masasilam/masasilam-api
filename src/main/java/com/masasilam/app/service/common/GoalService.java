package com.masasilam.app.service.common;

import com.masasilam.app.model.dto.request.GoalRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.GoalsResponse;

public interface GoalService {
    DataResponse<GoalsResponse> getGoals();
    DataResponse<Void> createGoal(GoalRequest request);
    DataResponse<Void> updateGoal(Long id, GoalRequest request);
    DataResponse<Void> deleteGoal(Long id);
}