package com.masasilam.app.service.common;

import com.masasilam.app.model.dto.response.AchievementsResponse;
import com.masasilam.app.model.dto.response.AnnotationsPageResponse;
import com.masasilam.app.model.dto.response.BookRecommendationResponse;
import com.masasilam.app.model.dto.response.CalendarResponse;
import com.masasilam.app.model.dto.response.DashboardMainResponse;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.ExportJobResponse;
import com.masasilam.app.model.dto.response.LibraryPageResponse;
import com.masasilam.app.model.dto.response.QuickStatsResponse;
import com.masasilam.app.model.dto.response.ReadingHistoryPageResponse;
import com.masasilam.app.model.dto.response.StatisticsResponse;
import com.masasilam.app.model.dto.response.UserReviewItemResponse;

import java.util.List;

public interface DashboardService {
    DataResponse<DashboardMainResponse> getMainDashboard();
    DataResponse<LibraryPageResponse> getLibrary(String filter, int page, int limit, String sortBy);
    DataResponse<ReadingHistoryPageResponse> getReadingHistory(int days, int page, int limit);
    DataResponse<StatisticsResponse> getStatistics(int period);
    DataResponse<AnnotationsPageResponse> getAnnotations(String type, int page, int limit, String sortBy);
    DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit);
    DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(int limit);
    DataResponse<QuickStatsResponse> getQuickStats();
    DataResponse<CalendarResponse> getCalendar(int year, int month);
    DataResponse<AchievementsResponse> getAchievements();
    DataResponse<ExportJobResponse> exportUserReadingData(String format);
}