package com.naskah.app.mapper;

import com.naskah.app.model.entity.ZineReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZineReviewFeedbackMapper {
    void insert(ZineReviewFeedback feedback);
    void update(ZineReviewFeedback feedback);
    void delete(Long id);

    ZineReviewFeedback findByUserAndReview(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}