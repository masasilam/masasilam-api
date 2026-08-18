package com.masasilam.app.service.trending.impl;

import com.masasilam.app.mapper.trending.TrendingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TrendingRefreshExecutor {
    private final TrendingMapper trendingMapper;

    @Transactional
    public void refreshType(String type, int windowDays, double viewWeight, double downloadWeight, double halfLifeDays, int storeLimit) {
        trendingMapper.deleteByType(type);
        switch (type) {
            case "BOOK" -> trendingMapper.insertBookTrending(windowDays, viewWeight, downloadWeight, halfLifeDays, storeLimit);
            case "ZINE" -> trendingMapper.insertZineTrending(windowDays, viewWeight, downloadWeight, halfLifeDays, storeLimit);
            case "FILM" -> trendingMapper.insertFilmTrending(windowDays, viewWeight, downloadWeight, halfLifeDays, storeLimit);
            case "NEWSPAPER" -> trendingMapper.insertNewspaperTrending(windowDays, viewWeight, downloadWeight, halfLifeDays, storeLimit);
            default -> throw new IllegalArgumentException("Unknown content type: " + type);
        }
    }
}