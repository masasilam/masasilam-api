package com.masasilam.app.controller;

import com.masasilam.app.model.dto.request.GoalRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.GoalsResponse;
import com.masasilam.app.service.common.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<DataResponse<GoalsResponse>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals());
    }

    @PostMapping
    public ResponseEntity<DataResponse<Void>> createGoal(@RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.createGoal(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> updateGoal(@PathVariable Long id,
                                                         @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> deleteGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.deleteGoal(id));
    }
}