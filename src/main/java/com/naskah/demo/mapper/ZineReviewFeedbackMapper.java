package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ZineReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZineReviewFeedbackMapper {
    void insert(ZineReviewFeedback feedback);
    void update(ZineReviewFeedback feedback);
    void delete(Long id);

    ZineReviewFeedback findByUserAndReview(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}