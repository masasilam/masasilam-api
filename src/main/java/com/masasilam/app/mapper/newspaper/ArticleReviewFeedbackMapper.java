package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.entity.newspaper.ArticleReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleReviewFeedbackMapper {
    void insert(ArticleReviewFeedback feedback);
    void update(ArticleReviewFeedback feedback);
    void delete(@Param("id") Long id);
    ArticleReviewFeedback findByUserAndReview(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}