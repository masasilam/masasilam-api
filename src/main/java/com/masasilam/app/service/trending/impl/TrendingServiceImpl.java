package com.masasilam.app.service.trending.impl;

import com.masasilam.app.mapper.trending.TrendingMapper;
import com.masasilam.app.model.dto.response.TrendingItemResponse;
import com.masasilam.app.service.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {
    private final TrendingMapper trendingMapper;
    private final TrendingRefreshExecutor refreshExecutor;
    private static final int WINDOW_DAYS = 7;
    private static final int STORE_LIMIT = 30;
    private static final int PER_CATEGORY_LIMIT = 10;
    private static final double VIEW_WEIGHT = 1.0;
    private static final double READ_WEIGHT = 5.0;
    private static final double DOWNLOAD_WEIGHT = 3.0;
    private static final double HALF_LIFE_DAYS = 2.0;
    private static final double QUALITY_MULTIPLIER_MIN = 0.85;
    private static final double QUALITY_MULTIPLIER_MAX = 1.15;
    private static final int QUALITY_MIN_RATINGS_FOR_TRUST = 10;

    @Override
    public List<TrendingItemResponse> getTrendingByType(String contentType, int limit) {
        return trendingMapper.findByType(contentType.toUpperCase(), limit);
    }

    @Override
    public Map<String, List<TrendingItemResponse>> getTrendingPerCategory() {
        List<TrendingItemResponse> all = trendingMapper.findAll(PER_CATEGORY_LIMIT);
        return all.stream().collect(Collectors.groupingBy(TrendingItemResponse::getContentType));
    }

    @Override
    public List<TrendingItemResponse> getTrendingOverall(int limit) {
        return trendingMapper.findTopOverall(limit);
    }

    @Override
    public void refreshAll() {
        refreshType("BOOK");
        refreshType("ZINE");
        refreshType("FILM");
        refreshType("NEWSPAPER");
    }

    private void refreshType(String type) {
        try {
            refreshExecutor.refreshType(type, WINDOW_DAYS, VIEW_WEIGHT, READ_WEIGHT, DOWNLOAD_WEIGHT,
                    HALF_LIFE_DAYS, QUALITY_MULTIPLIER_MIN, QUALITY_MULTIPLIER_MAX,
                    QUALITY_MIN_RATINGS_FOR_TRUST, STORE_LIMIT);
            log.info("[Trending] Refreshed {}", type);
        } catch (Exception e) {
            log.error("[Trending] Failed to refresh {}: {}", type, e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledRefresh() {
        log.info("[Trending] Starting scheduled refresh");
        refreshAll();
    }
}