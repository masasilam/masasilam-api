package com.masasilam.app.mapper.zine;

import com.masasilam.app.model.entity.ZineReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZineReviewMapper {
    void insert(ZineReview review);
    void update(ZineReview review);
    void softDelete(Long id);
    ZineReview findById(Long id);
    ZineReview findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
    List<ZineReview> findByZineWithPagination(@Param("zineId") Long zineId, @Param("offset") int offset, @Param("limit") int limit, @Param("sortBy") String sortBy);
    int countByZine(Long zineId);
    int countByUser(Long userId);
    List<ZineReview> findByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}