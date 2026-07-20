package com.masasilam.app.service.zine;

import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.ZineDashboardDTOs.*;

public interface ZineDashboardService {
    DataResponse<ZineLibraryPageResponse> getZineLibrary(String filter, int page, int limit, String sortBy);
    DataResponse<ZineHistoryPageResponse> getZineReadingHistory(int days, int page, int limit);
    DataResponse<ZineStatisticsResponse> getZineStatistics(int period);
    DatatableResponse<UserZineReviewItemResponse> getUserZineReviews(int page, int limit);
    DataResponse<CombinedOverviewStats> getCombinedOverviewStats();
}