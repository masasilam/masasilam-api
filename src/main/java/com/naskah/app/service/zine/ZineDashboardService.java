package com.naskah.app.service.zine;

import com.naskah.app.model.dto.response.DataResponse;
import com.naskah.app.model.dto.response.DatatableResponse;
import com.naskah.app.model.dto.response.ZineDashboardDTOs.*;

public interface ZineDashboardService {

    /** Library zine user dengan filter dan sort */
    DataResponse<ZineLibraryPageResponse> getZineLibrary(
            String filter, int page, int limit, String sortBy);

    /** Riwayat sesi baca zine */
    DataResponse<ZineHistoryPageResponse> getZineReadingHistory(
            int days, int page, int limit);

    /** Statistik baca zine dalam periode tertentu */
    DataResponse<ZineStatisticsResponse> getZineStatistics(int period);

    /** Review-review zine yang pernah ditulis user */
    DatatableResponse<UserZineReviewItemResponse> getUserZineReviews(int page, int limit);

    /** Overview gabungan: buku + zine (untuk main dashboard) */
    DataResponse<CombinedOverviewStats> getCombinedOverviewStats();
}