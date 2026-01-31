package com.naskah.demo.service.newspaper.impl;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.model.entity.newspaper.*;
import com.naskah.demo.service.newspaper.NewspaperService;
import com.naskah.demo.util.HashUtil;
import com.naskah.demo.util.IPUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewspaperServiceImpl implements NewspaperService {

    private final NewspaperMapper newspaperMapper;
    private final ArticleRatingMapper articleRatingMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "olahraga", "Olahraga",
            "politik", "Politik",
            "ekonomi", "Ekonomi",
            "budaya", "Budaya",
            "pendidikan", "Pendidikan",
            "kesehatan", "Kesehatan",
            "teknologi", "Teknologi",
            "hiburan", "Hiburan"
    );

    // ============================================
    // OVERVIEW & METADATA
    // ============================================

    @Override
    public DataResponse<List<NewspaperCategoryResponse>> getAllCategories() {
        try {
            List<NewspaperCategoryResponse> categories = newspaperMapper.getAllCategories();

            categories.forEach(cat -> {
                cat.setName(CATEGORY_NAMES.getOrDefault(cat.getSlug(), cat.getSlug()));
                cat.setIcon(getCategoryIcon(cat.getSlug()));
                cat.setDescription(getCategoryDescription(cat.getSlug()));
            });

            log.info("Retrieved {} categories", categories.size());

            return new DataResponse<>(SUCCESS, "Categories retrieved successfully",
                    HttpStatus.OK.value(), categories);

        } catch (Exception e) {
            log.error("Error getting all categories", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DatatableResponse<NewspaperSourceResponse> getAllSources(int page, int limit, String search) {
        try {
            int offset = (page - 1) * limit;

            List<NewspaperSourceResponse> sources = newspaperMapper.getAllSources(offset, limit, search);
            int totalCount = newspaperMapper.countAllSources(search);

            PageDataResponse<NewspaperSourceResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, sources);

            log.info("Retrieved {} sources (page {}/{})", sources.size(), page,
                    (int) Math.ceil((double) totalCount / limit));

            return new DatatableResponse<>(SUCCESS, "Sources retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting all sources", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<NewspaperStatsResponse> getOverallStats() {
        try {
            NewspaperStatsResponse stats = newspaperMapper.getOverallStats();

            log.info("Retrieved overall stats: {} articles, {} sources",
                    stats.getTotalArticles(), stats.getTotalSources());

            return new DataResponse<>(SUCCESS, "Statistics retrieved successfully",
                    HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Error getting overall stats", e);
            throw new InternalServerErrorException();
        }
    }

    // ============================================
    // BROWSE BY CATEGORY
    // ============================================

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesByCategory(
            String categorySlug, int page, int limit, String sortBy,
            String sortOrder, NewspaperSearchCriteria criteria) {
        try {
            validateCategory(categorySlug);

            int offset = (page - 1) * limit;

            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesByCategory(
                    categorySlug, offset, limit, sortBy, sortOrder, criteria);

            int totalCount = newspaperMapper.countArticlesByCategory(categorySlug, criteria);

            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));

            PageDataResponse<NewspaperArticleResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, articles);

            log.info("Retrieved {} articles for category '{}' (page {}/{})",
                    articles.size(), categorySlug, page, (int) Math.ceil((double) totalCount / limit));

            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting articles by category: {}", categorySlug, e);
            throw e;
        }
    }

    // ============================================
    // BROWSE BY DATE
    // ============================================

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesByDate(
            LocalDate date, int page, int limit, String sortBy, String category) {
        try {
            int offset = (page - 1) * limit;

            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesByDate(
                    date, offset, limit, sortBy, category);

            int totalCount = newspaperMapper.countArticlesByDate(date, category);

            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));

            PageDataResponse<NewspaperArticleResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, articles);

            log.info("Retrieved {} articles for date {} (page {}/{})",
                    articles.size(), date, page, (int) Math.ceil((double) totalCount / limit));

            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting articles by date: {}", date, e);
            throw e;
        }
    }

    // ============================================
    // BROWSE BY CATEGORY + DATE
    // ============================================

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesByCategoryAndDate(
            String categorySlug, LocalDate date, int page, int limit,
            String sortBy, String source) {
        try {
            validateCategory(categorySlug);

            NewspaperSearchCriteria criteria = NewspaperSearchCriteria.builder()
                    .dateFrom(date)
                    .dateTo(date)
                    .source(source)
                    .build();

            return getArticlesByCategory(categorySlug, page, limit, sortBy, "DESC", criteria);

        } catch (Exception e) {
            log.error("Error getting articles by category and date: {} on {}",
                    categorySlug, date, e);
            throw e;
        }
    }

    // ============================================
    // ARTICLE DETAIL
    // ============================================

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> getArticleDetail(
            String categorySlug, LocalDate date, String articleSlug,
            HttpServletRequest request) {
        try {
            validateCategory(categorySlug);

            NewspaperArticle article = newspaperMapper.findArticleByCategoryDateAndSlug(
                    categorySlug, date, articleSlug);

            if (article == null) {
                throw new DataNotFoundException();
            }

            // Track view
            trackArticleView(article, request);

            // Get detailed response
            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySlug(articleSlug);

            // Enrich with user data
            Long currentUserId = getCurrentUserId();
            enrichArticleDetailResponse(detail, currentUserId);

            // Get related content
            detail.setRelatedArticles(getRelatedArticles(article.getId(), article.getCategory(), 5));
            detail.setSameDateArticles(getSameDateArticles(article.getId(), article.getPublishDate(), 5));

            log.info("Retrieved article detail: {} (views: {})",
                    detail.getTitle(), detail.getViewCount());

            return new DataResponse<>(SUCCESS, "Article detail retrieved successfully",
                    HttpStatus.OK.value(), detail);

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting article detail: {}/{}/{}",
                    categorySlug, date, articleSlug, e);
            throw new InternalServerErrorException();
        }
    }

    // ============================================
    // SEARCH
    // ============================================

    @Override
    public DatatableResponse<NewspaperArticleResponse> searchArticles(
            NewspaperSearchCriteria criteria, int page, int limit) {
        try {
            if (criteria.getSearchQuery() == null || criteria.getSearchQuery().trim().isEmpty()) {
                throw new IllegalArgumentException("Search query is required");
            }

            int offset = (page - 1) * limit;

            List<NewspaperArticleResponse> articles = newspaperMapper.searchArticles(
                    criteria, offset, limit);

            int totalCount = newspaperMapper.countSearchArticles(criteria);

            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));

            PageDataResponse<NewspaperArticleResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, articles);

            log.info("Search '{}' returned {} results (page {}/{})",
                    criteria.getSearchQuery(), totalCount, page,
                    (int) Math.ceil((double) totalCount / limit));

            return new DatatableResponse<>(SUCCESS, "Search completed successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error searching articles: {}", criteria.getSearchQuery(), e);
            throw e;
        }
    }

    // ============================================
    // ON THIS DAY
    // ============================================

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesOnThisDay(
            int month, int day, int page, int limit) {
        try {
            int offset = (page - 1) * limit;

            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesOnThisDay(
                    month, day, offset, limit);

            int totalCount = newspaperMapper.countArticlesOnThisDay(month, day);

            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));

            PageDataResponse<NewspaperArticleResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, articles);

            log.info("Retrieved {} articles for this day in history: {}/{}",
                    totalCount, day, month);

            return new DatatableResponse<>(SUCCESS,
                    "Historical articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting articles on this day: {}/{}", month, day, e);
            throw new InternalServerErrorException();
        }
    }

    // ============================================
    // ANALYTICS
    // ============================================

    @Override
    public DataResponse<NewspaperAnalyticsResponse> getAnalyticsOverview(
            LocalDate dateFrom, LocalDate dateTo) {
        try {
            if (dateFrom == null) {
                dateFrom = LocalDate.now().minusDays(30);
            }
            if (dateTo == null) {
                dateTo = LocalDate.now();
            }

            NewspaperAnalyticsResponse analytics = newspaperMapper.getAnalyticsOverview(
                    dateFrom, dateTo);

            log.info("Retrieved analytics for period: {} to {}", dateFrom, dateTo);

            return new DataResponse<>(SUCCESS, "Analytics retrieved successfully",
                    HttpStatus.OK.value(), analytics);

        } catch (Exception e) {
            log.error("Error getting analytics overview", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<NewspaperArticleResponse>> getTrendingArticles(
            int days, int limit) {
        try {
            List<NewspaperArticleResponse> trending = newspaperMapper.getTrendingArticles(
                    days, limit);

            Long currentUserId = getCurrentUserId();
            trending.forEach(article -> enrichArticleResponse(article, currentUserId));

            log.info("Retrieved {} trending articles for last {} days",
                    trending.size(), days);

            return new DataResponse<>(SUCCESS, "Trending articles retrieved successfully",
                    HttpStatus.OK.value(), trending);

        } catch (Exception e) {
            log.error("Error getting trending articles", e);
            throw new InternalServerErrorException();
        }
    }

    // ============================================
    // CREATE & UPDATE
    // ============================================

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> createArticle(CreateArticleRequest request) {
        try {
            validateCategory(request.getCategory());

            if (newspaperMapper.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException("Article with this slug already exists");
            }

            // Convert HTML to plain text
            String plainText = convertHtmlToPlainText(request.getHtmlContent());
            int wordCount = calculateWordCount(plainText);

            NewspaperArticle article = NewspaperArticle.builder()
                    .sourceId(request.getSourceId())
                    .slug(request.getSlug())
                    .category(request.getCategory())
                    .publishDate(request.getPublishDate())
                    .title(request.getTitle())
                    .content(plainText)  // Auto-generated
                    .htmlContent(request.getHtmlContent())
                    .wordCount(wordCount)
                    .author(request.getAuthor())
                    .pageNumber(request.getPageNumber())
                    .importance(request.getImportance() != null ? request.getImportance() : "medium")
                    .imageUrl(request.getImageUrl())
                    .parentArticleId(request.getParentArticleId())
                    .articleLevel(request.getArticleLevel() != null ? request.getArticleLevel() : 0)
                    .isActive(true)
                    .isFeatured(false)
                    .build();

            newspaperMapper.insertArticle(article);
            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySlug(article.getSlug());

            log.info("Article created: {} (ID: {})", article.getTitle(), article.getId());

            return new DataResponse<>(SUCCESS, "Article created successfully",
                    HttpStatus.CREATED.value(), detail);

        } catch (Exception e) {
            log.error("Error creating article", e);
            throw new InternalServerErrorException();
        }
    }

    // Tambahkan helper method
    private String convertHtmlToPlainText(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        // Remove HTML tags
        String text = html.replaceAll("<[^>]*>", "");
        // Decode HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        // Clean up whitespace
        return text.trim().replaceAll("\\s+", " ");
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> updateArticle(Long id, UpdateArticleRequest request) {
        try {
            NewspaperArticle existing = newspaperMapper.findById(id);
            if (existing == null) {
                throw new DataNotFoundException();
            }

            int wordCount = calculateWordCount(request.getContent());

            existing.setTitle(request.getTitle());
            existing.setContent(request.getContent());
            existing.setHtmlContent(request.getHtmlContent());
            existing.setWordCount(wordCount);
            existing.setAuthor(request.getAuthor());
            existing.setPageNumber(request.getPageNumber());
            existing.setImportance(request.getImportance());
            existing.setImageUrl(request.getImageUrl());

            newspaperMapper.updateArticle(existing);

            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySlug(existing.getSlug());

            log.info("Article updated: {} (ID: {})", existing.getTitle(), id);

            return new DataResponse<>(SUCCESS, "Article updated successfully",
                    HttpStatus.OK.value(), detail);

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating article ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    @Transactional
    private void trackArticleView(NewspaperArticle article, HttpServletRequest request) {
        try {
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();

            String viewerHash = HashUtil.generateViewerHash(
                    article.getSlug(), userId, ipAddress, userAgent);

            boolean hasViewed = newspaperMapper.hasViewByHash(viewerHash, "view");

            if (!hasViewed) {
                ArticleView view = ArticleView.builder()
                        .articleId(article.getId())
                        .userId(userId)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .viewerHash(viewerHash)
                        .actionType("view")
                        .build();

                newspaperMapper.insertArticleView(view);
                newspaperMapper.incrementViewCount(article.getId());

                log.debug("New view recorded for article: {}", article.getTitle());
            }
        } catch (Exception e) {
            log.error("Error tracking article view", e);
        }
    }

    private void enrichArticleResponse(NewspaperArticleResponse article, Long userId) {
        article.setCategoryName(CATEGORY_NAMES.getOrDefault(
                article.getCategory(), article.getCategory()));

        article.setDateFormatted(article.getPublishDate().format(DATE_FORMATTER));

        if (userId != null) {
            article.setIsSaved(newspaperMapper.isArticleSavedByUser(article.getId(), userId));

            ArticleRating rating = articleRatingMapper.findByUserAndArticle(userId, article.getId());
            article.setMyRating(rating != null ? rating.getRating() : null);
        } else {
            article.setIsSaved(false);
            article.setMyRating(null);
        }
    }

    private void enrichArticleDetailResponse(NewspaperArticleDetailResponse detail, Long userId) {
        detail.setCategoryName(CATEGORY_NAMES.getOrDefault(detail.getCategory(), detail.getCategory()));
        detail.setDateFormatted(detail.getPublishDate().format(DATE_FORMATTER));

        if (userId != null) {
            detail.setIsSaved(newspaperMapper.isArticleSavedByUser(detail.getId(), userId));

            ArticleRating rating = articleRatingMapper.findByUserAndArticle(userId, detail.getId());
            detail.setMyRating(rating != null ? rating.getRating() : null);

            detail.setHasReviewed(newspaperMapper.hasUserReviewedArticle(userId, detail.getId()));
        } else {
            detail.setIsSaved(false);
            detail.setMyRating(null);
            detail.setHasReviewed(false);
        }
    }

    private List<NewspaperArticleResponse> getRelatedArticles(
            Long articleId, String category, int limit) {
        try {
            return newspaperMapper.getRelatedArticles(articleId, category, limit);
        } catch (Exception e) {
            log.error("Error getting related articles", e);
            return Collections.emptyList();
        }
    }

    private List<NewspaperArticleResponse> getSameDateArticles(
            Long articleId, LocalDate date, int limit) {
        try {
            return newspaperMapper.getSameDateArticles(articleId, date, limit);
        } catch (Exception e) {
            log.error("Error getting same date articles", e);
            return Collections.emptyList();
        }
    }

    private Long getCurrentUserId() {
        try {
            String username = headerHolder.getUsername();
            if (username != null && !username.isEmpty()) {
                User user = userMapper.findUserByUsername(username);
                return user != null ? user.getId() : null;
            }
            return null;
        } catch (Exception e) {
            log.debug("No authenticated user found");
            return null;
        }
    }

    private void validateCategory(String categorySlug) {
        if (!CATEGORY_NAMES.containsKey(categorySlug)) {
            throw new IllegalArgumentException("Invalid category: " + categorySlug);
        }
    }

    private int calculateWordCount(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }

    private String getCategoryIcon(String category) {
        return switch (category) {
            case "olahraga" -> "⚽";
            case "politik" -> "🏛️";
            case "ekonomi" -> "💰";
            case "budaya" -> "🎭";
            case "pendidikan" -> "📚";
            case "kesehatan" -> "🏥";
            case "teknologi" -> "💻";
            case "hiburan" -> "🎬";
            default -> "📰";
        };
    }

    private String getCategoryDescription(String category) {
        return switch (category) {
            case "olahraga" -> "Berita dan artikel tentang dunia olahraga";
            case "politik" -> "Berita politik dan pemerintahan";
            case "ekonomi" -> "Berita ekonomi dan bisnis";
            case "budaya" -> "Berita seni dan budaya";
            case "pendidikan" -> "Berita pendidikan dan akademis";
            case "kesehatan" -> "Berita kesehatan dan medis";
            case "teknologi" -> "Berita teknologi dan inovasi";
            case "hiburan" -> "Berita hiburan dan gaya hidup";
            default -> "Artikel berita umum";
        };
    }
}