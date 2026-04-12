package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.GoalRequest;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.GoalsResponse;
import com.naskah.demo.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    // GET /api/dashboard/goals
    @GetMapping
    public ResponseEntity<DataResponse<GoalsResponse>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals());
    }

    // POST /api/dashboard/goals
    @PostMapping
    public ResponseEntity<DataResponse<Void>> createGoal(@RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.createGoal(request));
    }

    // PUT /api/dashboard/goals/{id}
    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> updateGoal(
            @PathVariable Long id,
            @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, request));
    }

    // DELETE /api/dashboard/goals/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> deleteGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.deleteGoal(id));
    }
}