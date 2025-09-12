package com.naskah.demo.controller;

import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserDashboardResponse;
import com.naskah.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DataResponse<UserDashboardResponse>> getUserDashboard() {
        DataResponse<UserDashboardResponse> response = dashboardService.getUserDashboard();
        return ResponseEntity.ok(response);
    }
}
