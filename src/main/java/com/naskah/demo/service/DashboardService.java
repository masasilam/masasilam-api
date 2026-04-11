package com.naskah.demo.service;

import com.naskah.demo.model.dto.response.*;

import java.util.List;

/**
 * DashboardService — interface yang diimplementasikan DashboardServiceImpl.
 *
 * Setiap signature harus cocok persis dengan:
 *   1. DashboardServiceImpl (implementasi)
 *   2. DashboardController (pemanggil)
 */
public interface DashboardService {

    // ── Main dashboard ────────────────────────────────────────────────────────
    DataResponse<DashboardMainResponse> getMainDashboard();

    // ── Library ───────────────────────────────────────────────────────────────
    DataResponse<LibraryPageResponse> getLibrary(
            String filter, int page, int limit, String sortBy);

    // ── Reading history ───────────────────────────────────────────────────────
    DataResponse<ReadingHistoryPageResponse> getReadingHistory(
            int days, int page, int limit);

    // ── Statistics ────────────────────────────────────────────────────────────
    DataResponse<StatisticsResponse> getStatistics(int period);

    // ── Annotations ───────────────────────────────────────────────────────────
    DataResponse<AnnotationsPageResponse> getAnnotations(
            String type, int page, int limit, String sortBy);

    // ── Reviews ───────────────────────────────────────────────────────────────
    // Dipanggil dari DashboardController.getUserReviews()
    DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit);

    // ── Goals ─────────────────────────────────────────────────────────────────
    DataResponse<GoalsResponse> getGoals();

    // ── Recommendations ───────────────────────────────────────────────────────
    // Dipanggil dari DashboardController.getRecommendations()
    DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(int limit);

    // ── Quick stats ───────────────────────────────────────────────────────────
    // Dipanggil dari DashboardController.getQuickStats()
    DataResponse<QuickStatsResponse> getQuickStats();

    // ── Calendar ──────────────────────────────────────────────────────────────
    DataResponse<CalendarResponse> getCalendar(int year, int month);

    // ── Achievements ──────────────────────────────────────────────────────────
    DataResponse<AchievementsResponse> getAchievements();

    // ── Export ────────────────────────────────────────────────────────────────
    // Dipanggil dari DashboardController.exportUserData()
    DataResponse<ExportJobResponse> exportUserReadingData(String format);
}