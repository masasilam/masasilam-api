package com.naskah.demo.service;

import com.naskah.demo.model.dto.response.*;
import java.util.List;

/**
 * DashboardService - Interface untuk semua fitur dashboard
 */
public interface DashboardService {

    /**
     * Get main dashboard overview
     */
    DataResponse<UserReadingDashboardResponse> getUserReadingDashboard();

    /**
     * Get user's library with filters
     */
    DatatableResponse<BookLibraryItemResponse> getUserLibrary(
            String filter, int page, int limit, String sortBy);

    /**
     * Get reading history/activity timeline
     */
    DatatableResponse<ReadingActivityResponse> getReadingHistory(
            int days, int page, int limit);

    /**
     * Get reading statistics
     */
    DataResponse<ReadingStatisticsResponse> getReadingStatistics(int period);

    /**
     * Get all user annotations
     */
    DatatableResponse<AnnotationItemResponse> getAllAnnotations(
            String type, int page, int limit, String sortBy);

    /**
     * Get all user reviews
     */
    DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit);

    /**
     * Get user's reading goals
     */
    DataResponse<ReadingGoalsResponse> getReadingGoals();

    /**
     * Get personalized book recommendations
     */
    DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(int limit);

    /**
     * Get quick stats (lightweight)
     */
    DataResponse<QuickStatsResponse> getQuickStats();

    /**
     * Get reading calendar
     */
    DataResponse<ReadingCalendarResponse> getReadingCalendar(
            Integer year, Integer month);

    /**
     * Get user achievements
     */
    DataResponse<List<AchievementResponse>> getUserAchievements();

    /**
     * Export user reading data
     */
    DataResponse<ExportJobResponse> exportUserReadingData(String format);
}
