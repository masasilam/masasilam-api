package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.dto.newspaper.ArticleRatingStatsResponse;
import com.masasilam.app.model.entity.newspaper.ArticleRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleRatingMapper {
    void insert(ArticleRating rating);
    void update(ArticleRating rating);
    void delete(@Param("id") Long id);
    ArticleRating findByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
    ArticleRatingStatsResponse getArticleRatingStats(@Param("articleId") Long articleId);
}