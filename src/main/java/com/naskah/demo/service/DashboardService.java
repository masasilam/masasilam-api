package com.naskah.demo.service;

import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserDashboardResponse;

public interface DashboardService {
    DataResponse<UserDashboardResponse> getUserDashboard();
}
