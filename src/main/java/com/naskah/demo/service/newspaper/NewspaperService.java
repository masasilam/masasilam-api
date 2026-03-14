package com.naskah.demo.service.newspaper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface NewspaperService {

    // Metadata
    DataResponse<List<NewspaperCategoryResponse>> getAllCategories();
    DatatableResponse<NewspaperSourceResponse> getAllSources(int page, int limit, String search);
    DataResponse<NewspaperStatsResponse> getOverallStats();

    // Browse
    DatatableResponse<NewspaperArticleResponse> getArticlesByCategory(
            String categorySlug, int page, int limit, String sortBy,
            String sortOrder, NewspaperSearchCriteria criteria);
    DatatableResponse<NewspaperArticleResponse> getArticlesByDate(
            LocalDate date, int page, int limit, String sortBy, String category);
    DatatableResponse<NewspaperArticleResponse> getArticlesByCategoryAndDate(
            String categorySlug, LocalDate date, int page, int limit,
            String sortBy, String source);

    // Detail
    DataResponse<NewspaperArticleDetailResponse> getArticleDetail(
            String categorySlug, LocalDate date, String articleSlug,
            HttpServletRequest request);

    // FIX: tambah getArticleById untuk form edit
    DataResponse<NewspaperArticleDetailResponse> getArticleById(Long id);

    // Search
    DatatableResponse<NewspaperArticleResponse> searchArticles(
            NewspaperSearchCriteria criteria, int page, int limit);

    // On This Day
    DatatableResponse<NewspaperArticleResponse> getArticlesOnThisDay(
            int month, int day, int page, int limit);

    // Analytics
    DataResponse<NewspaperAnalyticsResponse> getAnalyticsOverview(
            LocalDate dateFrom, LocalDate dateTo);
    DataResponse<List<NewspaperArticleResponse>> getTrendingArticles(int days, int limit);

    // CRUD
    DataResponse<NewspaperArticleDetailResponse> createArticle(CreateArticleRequest request);
    DataResponse<NewspaperArticleDetailResponse> updateArticle(Long id, UpdateArticleRequest request);

    // FIX: tambah deleteArticle
    DataResponse<Void> deleteArticle(Long id);
}