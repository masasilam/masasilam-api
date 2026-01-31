package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.newspaper.ArticleReviewFeedback;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ArticleReviewFeedbackMapper {

    /**
     * Insert new feedback
     */
    @Insert("INSERT INTO article_review_feedback (user_id, review_id, is_helpful, created_at, updated_at) " +
            "VALUES (#{userId}, #{reviewId}, #{isHelpful}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleReviewFeedback feedback);

    /**
     * Update feedback
     */
    @Update("UPDATE article_review_feedback SET is_helpful = #{isHelpful}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void update(ArticleReviewFeedback feedback);

    /**
     * Delete feedback
     */
    @Delete("DELETE FROM article_review_feedback WHERE id = #{id}")
    void delete(@Param("id") Long id);

    /**
     * Find feedback by user and review
     */
    @Select("SELECT * FROM article_review_feedback " +
            "WHERE user_id = #{userId} AND review_id = #{reviewId}")
    ArticleReviewFeedback findByUserAndReview(
            @Param("userId") Long userId,
            @Param("reviewId") Long reviewId);
}
