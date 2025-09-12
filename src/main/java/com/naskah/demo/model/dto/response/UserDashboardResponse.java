package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class UserDashboardResponse {
    private Long userId;
    private List<ProjectResponse> createdProjects;
    private List<ProjectMembershipResponse> joinedProjects;
    private List<ProjectResponse> followedProjects;
    private List<NotificationResponse> recentNotifications;
    private UserContributionStats contributionStats;
    private List<UserActivityResponse> recentActivity;
}