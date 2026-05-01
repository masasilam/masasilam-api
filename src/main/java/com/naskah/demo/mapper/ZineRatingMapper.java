package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ZineRatingStatsResponse;
import com.naskah.demo.model.entity.ZineRating;
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