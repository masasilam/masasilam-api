package com.naskah.app.mapper;

import com.naskah.app.model.dto.response.ZineRatingStatsResponse;
import com.naskah.app.model.entity.ZineRating;
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
    List<Map<String, Object>> findAllUserRatings(Long userId);
}