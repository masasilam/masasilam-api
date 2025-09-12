//package com.naskah.demo.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/statistics")
//@RequiredArgsConstructor
//public class StatisticsController {
//
//    @GetMapping("/dashboard")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(
//            @AuthenticationPrincipal User currentUser) {
//
//        Map<String, Object> stats = new HashMap<>();
//
//        // User statistics
//        Map<String, Object> userStats = new HashMap<>();
//        userStats.put("pagesCompleted", currentUser.getPagesCompleted());
//        userStats.put("projectsCompleted", currentUser.getProjectsCompleted());
//        userStats.put("level", currentUser.getLevel());
//        userStats.put("experiencePoints", currentUser.getExperiencePoints());
//        userStats.put("averageRating", currentUser.getAverageRating());
//
//        // System statistics (mock data - implement real queries)
//        Map<String, Object> systemStats = new HashMap<>();
//        systemStats.put("totalProjects", 150);
//        systemStats.put("activeProjects", 25);
//        systemStats.put("publishedBooks", 120);
//        systemStats.put("activeVolunteers", 45);
//        systemStats.put("totalPages", 15000);
//
//        stats.put("user", userStats);
//        stats.put("system", systemStats);
//
//        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard statistics retrieved"));
//    }
//
//    @GetMapping("/leaderboard")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeaderboard() {
//
//        // Mock leaderboard data - implement real queries
//        Map<String, Object> leaderboard = new HashMap<>();
//
//        // Top contributors by pages
//        // Top contributors by projects
//        // Recent achievements
//
//        return ResponseEntity.ok(ApiResponse.success(leaderboard, "Leaderboard retrieved"));
//    }
//}
