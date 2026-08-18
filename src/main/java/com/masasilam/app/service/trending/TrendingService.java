package com.masasilam.app.service.trending;

import com.masasilam.app.model.dto.response.TrendingItemResponse;

import java.util.List;
import java.util.Map;

public interface TrendingService {
    List<TrendingItemResponse> getTrendingByType(String contentType, int limit);
    Map<String, List<TrendingItemResponse>> getTrendingPerCategory();
    List<TrendingItemResponse> getTrendingOverall(int limit);
    void refreshAll();
}