package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.newspaper.ArticleRatingStatsResponse;
import com.naskah.demo.model.entity.newspaper.ArticleRating;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ArticleRatingMapper {

    /**
     * Insert new rating
     */
    @Insert("INSERT INTO article_ratings (user_id, article_id, rating, created_at, updated_at) " +
            "VALUES (#{userId}, #{articleId}, #{rating}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleRating rating);

    /**
     * Update rating
     */
    @Update("UPDATE article_ratings SET rating = #{rating}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void update(ArticleRating rating);

    /**
     * Delete rating
     */
    @Delete("DELETE FROM article_ratings WHERE id = #{id}")
    void delete(@Param("id") Long id);

    /**
     * Find rating by user and article
     */
    @Select("SELECT * FROM article_ratings WHERE user_id = #{userId} AND article_id = #{articleId}")
    ArticleRating findByUserAndArticle(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId);

    /**
     * Get article rating statistics
     */
    @Select("SELECT " +
            "  article_id AS articleId, " +
            "  AVG(rating) AS averageRating, " +
            "  COUNT(*) AS totalRatings, " +
            "  SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) AS fiveStarCount, " +
            "  SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) AS fourStarCount, " +
            "  SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) AS threeStarCount, " +
            "  SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) AS twoStarCount, " +
            "  SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) AS oneStarCount " +
            "FROM article_ratings " +
            "WHERE article_id = #{articleId} " +
            "GROUP BY article_id")
    ArticleRatingStatsResponse getArticleRatingStats(@Param("articleId") Long articleId);
}
