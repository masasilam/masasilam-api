package com.masasilam.app.mapper.trending;

import com.masasilam.app.model.dto.response.TrendingItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrendingMapper {
    void deleteByType(@Param("contentType") String contentType);
    void insertBookTrending(@Param("days") int days, @Param("viewWeight") double viewWeight,
                            @Param("readWeight") double readWeight,
                            @Param("downloadWeight") double downloadWeight,
                            @Param("halfLifeDays") double halfLifeDays,
                            @Param("qualityMin") double qualityMin,
                            @Param("qualityMax") double qualityMax,
                            @Param("qualityMinRatings") int qualityMinRatings,
                            @Param("limit") int limit);
    void insertZineTrending(@Param("days") int days, @Param("viewWeight") double viewWeight,
                            @Param("readWeight") double readWeight,
                            @Param("downloadWeight") double downloadWeight,
                            @Param("halfLifeDays") double halfLifeDays,
                            @Param("qualityMin") double qualityMin,
                            @Param("qualityMax") double qualityMax,
                            @Param("qualityMinRatings") int qualityMinRatings,
                            @Param("limit") int limit);
    void insertFilmTrending(@Param("days") int days, @Param("viewWeight") double viewWeight,
                            @Param("downloadWeight") double downloadWeight,
                            @Param("halfLifeDays") double halfLifeDays, @Param("limit") int limit);
    void insertNewspaperTrending(@Param("days") int days, @Param("viewWeight") double viewWeight,
                                 @Param("downloadWeight") double downloadWeight,
                                 @Param("halfLifeDays") double halfLifeDays, @Param("limit") int limit);
    List<TrendingItemResponse> findByType(@Param("contentType") String contentType, @Param("limit") int limit);
    List<TrendingItemResponse> findAll(@Param("limitPerType") int limitPerType);
    List<TrendingItemResponse> findTopOverall(@Param("limit") int limit);
}