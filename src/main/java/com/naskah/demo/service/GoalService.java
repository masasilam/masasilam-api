package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.GoalRequest;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.GoalsResponse;

public interface GoalService {
    DataResponse<GoalsResponse> getGoals();
    DataResponse<Void>          createGoal(GoalRequest request);
    DataResponse<Void>          updateGoal(Long id, GoalRequest request);
    DataResponse<Void>          deleteGoal(Long id);
}