package com.masasilam.app.mapper.zine;

import com.masasilam.app.model.entity.ZineReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZineReviewFeedbackMapper {
    void insert(ZineReviewFeedback feedback);
    void update(ZineReviewFeedback feedback);
    void delete(Long id);
    ZineReviewFeedback findByUserAndReview(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}