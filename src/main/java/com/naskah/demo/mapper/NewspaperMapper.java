package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.entity.newspaper.*;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NewspaperMapper {

    // ============================================
    // CATEGORIES & SOURCES
    // ============================================

    List<NewspaperCategoryResponse> getAllCategories();

    List<NewspaperSourceResponse> getAllSources(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("search") String search);

    int countAllSources(@Param("search") String search);

    NewspaperStatsResponse getOverallStats();

    // ============================================
    // ARTICLE QUERIES
    // ============================================

    List<NewspaperArticleResponse> getArticlesByCategory(
            @Param("categorySlug") String categorySlug,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder,
            @Param("criteria") NewspaperSearchCriteria criteria);

    int countArticlesByCategory(
            @Param("categorySlug") String categorySlug,
            @Param("criteria") NewspaperSearchCriteria criteria);

    List<NewspaperArticleResponse> getArticlesByDate(
            @Param("date") LocalDate date,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("category") String category);

    int countArticlesByDate(
            @Param("date") LocalDate date,
            @Param("category") String category);

    // ============================================
    // ARTICLE DETAIL
    // ============================================

    NewspaperArticle findArticleByCategoryDateAndSlug(
            @Param("categorySlug") String categorySlug,
            @Param("date") LocalDate date,
            @Param("articleSlug") String articleSlug);

    NewspaperArticleDetailResponse getArticleDetailBySlug(@Param("slug") String slug);

    // ============================================
    // SEARCH
    // ============================================

    List<NewspaperArticleResponse> searchArticles(
            @Param("criteria") NewspaperSearchCriteria criteria,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countSearchArticles(@Param("criteria") NewspaperSearchCriteria criteria);

    // ============================================
    // ON THIS DAY
    // ============================================

    List<NewspaperArticleResponse> getArticlesOnThisDay(
            @Param("month") int month,
            @Param("day") int day,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countArticlesOnThisDay(
            @Param("month") int month,
            @Param("day") int day);

    // ============================================
    // ANALYTICS
    // ============================================

    NewspaperAnalyticsResponse getAnalyticsOverview(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);

    List<NewspaperArticleResponse> getTrendingArticles(
            @Param("days") int days,
            @Param("limit") int limit);

    // ============================================
    // RELATED ARTICLES
    // ============================================

    List<NewspaperArticleResponse> getRelatedArticles(
            @Param("articleId") Long articleId,
            @Param("category") String category,
            @Param("limit") int limit);

    List<NewspaperArticleResponse> getSameDateArticles(
            @Param("articleId") Long articleId,
            @Param("date") LocalDate date,
            @Param("limit") int limit);

    // ============================================
    // USER INTERACTIONS
    // ============================================

    boolean isArticleSavedByUser(
            @Param("articleId") Long articleId,
            @Param("userId") Long userId);

    boolean hasUserReviewedArticle(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId);

    // ============================================
    // VIEW TRACKING
    // ============================================

    boolean hasViewByHash(
            @Param("viewerHash") String viewerHash,
            @Param("actionType") String actionType);

    void insertArticleView(ArticleView view);

    void incrementViewCount(@Param("articleId") Long articleId);

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    void insertArticle(NewspaperArticle article);

    void updateArticle(NewspaperArticle article);

    boolean existsBySlug(@Param("slug") String slug);

    NewspaperArticle findById(@Param("id") Long id);

    void incrementSaveCount(@Param("articleId") Long articleId);

    void decrementSaveCount(@Param("articleId") Long articleId);

    void incrementShareCount(@Param("articleId") Long articleId);
}