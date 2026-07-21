package com.masasilam.app.mapper.zine;

import com.masasilam.app.model.dto.response.ZineRatingStatsResponse;
import com.masasilam.app.model.entity.ZineRating;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ZineRatingMapper {
    void insert(ZineRating rating);
    void update(ZineRating rating);
    void delete(Long id);
    ZineRating findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
    ZineRatingStatsResponse getZineRatingStats(Long zineId);
    @MapKey("id")
    List<Map<String, Object>> findAllUserRatings(Long userId);
}