package com.naskah.demo.service.newspaper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface NewspaperService {

    DataResponse<NewspaperArticleDetailResponse> createArticle(CreateArticleRequest request);

    DataResponse<NewspaperArticleDetailResponse> updateArticle(Long id, UpdateArticleRequest request);

    DataResponse<List<NewspaperCategoryResponse>> getAllCategories();

    DatatableResponse<NewspaperSourceResponse> getAllSources(int page, int limit, String search);

    DataResponse<NewspaperStatsResponse> getOverallStats();

    DatatableResponse<NewspaperArticleResponse> getArticlesByCategory(
            String categorySlug,
            int page,
            int limit,
            String sortBy,
            String sortOrder,
            NewspaperSearchCriteria criteria);

    DatatableResponse<NewspaperArticleResponse> getArticlesByDate(
            LocalDate date,
            int page,
            int limit,
            String sortBy,
            String category);

    DatatableResponse<NewspaperArticleResponse> getArticlesByCategoryAndDate(
            String categorySlug,
            LocalDate date,
            int page,
            int limit,
            String sortBy,
            String source);

    DataResponse<NewspaperArticleDetailResponse> getArticleDetail(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            HttpServletRequest request);

    DatatableResponse<NewspaperArticleResponse> searchArticles(
            NewspaperSearchCriteria criteria,
            int page,
            int limit);

    DatatableResponse<NewspaperArticleResponse> getArticlesOnThisDay(
            int month,
            int day,
            int page,
            int limit);

    DataResponse<NewspaperAnalyticsResponse> getAnalyticsOverview(
            LocalDate dateFrom,
            LocalDate dateTo);

    DataResponse<List<NewspaperArticleResponse>> getTrendingArticles(
            int days,
            int limit);
}