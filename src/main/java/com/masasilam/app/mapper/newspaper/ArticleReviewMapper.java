package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.entity.newspaper.ArticleReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleReviewMapper {
    void insert(ArticleReview review);
    void update(ArticleReview review);
    void softDelete(@Param("id") Long id);
    ArticleReview findById(@Param("id") Long id);
    ArticleReview findByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
    List<ArticleReview> findByArticleWithPagination(@Param("articleId") Long articleId, @Param("offset") int offset, @Param("limit") int limit, @Param("sortBy") String sortBy);
    int countByArticle(@Param("articleId") Long articleId);
}