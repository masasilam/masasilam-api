package com.naskah.demo.service;

import com.naskah.demo.model.dto.response.*;
import java.util.List;

public interface DashboardService {
    DataResponse<UserReadingDashboardResponse> getUserReadingDashboard();
    DatatableResponse<BookLibraryItemResponse> getUserLibrary(String filter, int page, int limit, String sortBy);
    DatatableResponse<ReadingActivityResponse> getReadingHistory(int days, int page, int limit);
    DataResponse<ReadingStatisticsResponse> getReadingStatistics(int period);
    DatatableResponse<AnnotationItemResponse> getAllAnnotations(String type, int page, int limit, String sortBy);
    DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit);
    DataResponse<ReadingGoalsResponse> getReadingGoals();
    DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(int limit);
    DataResponse<QuickStatsResponse> getQuickStats();
    DataResponse<ReadingCalendarResponse> getReadingCalendar(Integer year, Integer month);
    DataResponse<List<AchievementResponse>> getUserAchievements();
    DataResponse<ExportJobResponse> exportUserReadingData(String format);
}
