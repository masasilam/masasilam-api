package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.newspaper.ArticleReview;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleReviewMapper {

    /**
     * Insert new review
     */
    @Insert("INSERT INTO article_reviews (user_id, article_id, title, content, " +
            "helpful_count, not_helpful_count, reply_count, created_at, updated_at) " +
            "VALUES (#{userId}, #{articleId}, #{title}, #{content}, " +
            "#{helpfulCount}, #{notHelpfulCount}, #{replyCount}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleReview review);

    /**
     * Update review
     */
    @Update("UPDATE article_reviews SET title = #{title}, content = #{content}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    void update(ArticleReview review);

    /**
     * Soft delete review
     */
    @Update("UPDATE article_reviews SET is_deleted = TRUE WHERE id = #{id}")
    void softDelete(@Param("id") Long id);

    /**
     * Find review by ID
     */
    @Select("SELECT * FROM article_reviews WHERE id = #{id} AND is_deleted = FALSE")
    ArticleReview findById(@Param("id") Long id);

    /**
     * Find review by user and article
     */
    @Select("SELECT * FROM article_reviews " +
            "WHERE user_id = #{userId} AND article_id = #{articleId} AND is_deleted = FALSE")
    ArticleReview findByUserAndArticle(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId);

    /**
     * Find reviews by article with pagination
     */
    List<ArticleReview> findByArticleWithPagination(
            @Param("articleId") Long articleId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy);

    /**
     * Count reviews by article
     */
    @Select("SELECT COUNT(*) FROM article_reviews " +
            "WHERE article_id = #{articleId} AND is_deleted = FALSE")
    int countByArticle(@Param("articleId") Long articleId);
}
