package com.masasilam.app.service.newspaper.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.*;
import com.masasilam.app.model.dto.newspaper.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.newspaper.*;
import com.masasilam.app.service.newspaper.NewspaperService;
import com.masasilam.app.util.HashUtil;
import com.masasilam.app.util.IPUtil;
import com.masasilam.app.util.interceptor.HeaderHolder;
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
    private static final String UNKNOWN = "Unknown";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    private static final Map<String, String> CATEGORY_NAMES = Map.ofEntries(
            Map.entry("nasional", "Nasional"),
            Map.entry("internasional", "Internasional"),
            Map.entry("daerah", "Daerah / Lokal"),
            Map.entry("politik", "Politik"),
            Map.entry("hukum", "Hukum & Kriminal"),
            Map.entry("pemerintahan", "Pemerintahan"),
            Map.entry("ekonomi", "Ekonomi"),
            Map.entry("bisnis", "Bisnis & Keuangan"),
            Map.entry("pertanian", "Pertanian"),
            Map.entry("sosial", "Sosial"),
            Map.entry("pendidikan", "Pendidikan"),
            Map.entry("kesehatan", "Kesehatan"),
            Map.entry("agama", "Agama"),
            Map.entry("lingkungan", "Lingkungan"),
            Map.entry("teknologi", "Teknologi"),
            Map.entry("sains", "Sains & Iptek"),
            Map.entry("budaya", "Budaya"),
            Map.entry("hiburan", "Hiburan"),
            Map.entry("olahraga", "Olahraga"),
            Map.entry("gaya-hidup", "Gaya Hidup"),
            Map.entry("kuliner", "Kuliner"),
            Map.entry("wisata", "Wisata"),
            Map.entry("opini", "Opini / Kolom"),
            Map.entry("sastra", "Sastra & Cerita"),
            Map.entry("cerita-bersambung", "Cerita Bersambung"),
            Map.entry("iklan", "Iklan / Pengumuman"),
            Map.entry("lainnya", "Lainnya")
    );

    @Override
    public DataResponse<List<NewspaperCategoryResponse>> getAllCategories() {
        try {
            List<NewspaperCategoryResponse> categories = newspaperMapper.getAllCategories();
            categories.forEach(cat -> {
                cat.setName(CATEGORY_NAMES.getOrDefault(cat.getSlug(), cat.getSlug()));
                cat.setIcon(getCategoryIcon(cat.getSlug()));
                cat.setDescription(getCategoryDescription(cat.getSlug()));
            });
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
            PageDataResponse<NewspaperSourceResponse> pageData = new PageDataResponse<>(page, limit, totalCount, sources);
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
            return new DataResponse<>(SUCCESS, "Statistics retrieved successfully",
                    HttpStatus.OK.value(), stats);
        } catch (Exception e) {
            log.error("Error getting overall stats", e);
            throw new InternalServerErrorException();
        }
    }

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
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error getting articles by category: {}", categorySlug, e);
            throw e;
        }
    }

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
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error getting articles by date: {}", date, e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesByCategoryAndDate(
            String categorySlug, LocalDate date, int page, int limit,
            String sortBy, String source) {
        try {
            validateCategory(categorySlug);
            NewspaperSearchCriteria criteria = NewspaperSearchCriteria.builder()
                    .dateFrom(date).dateTo(date).source(source).build();
            return getArticlesByCategory(categorySlug, page, limit, sortBy, "DESC", criteria);
        } catch (Exception e) {
            log.error("Error getting articles by category and date: {} on {}", categorySlug, date, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> getArticleDetail(
            String categorySlug, LocalDate date, String articleSlug,
            HttpServletRequest request) {
        try {
            validateCategory(categorySlug);
            NewspaperArticle article = newspaperMapper.findArticleByCategoryDateAndSlug(
                    categorySlug, date, articleSlug);
            if (article == null) throw new DataNotFoundException();

            trackArticleView(article, request);

            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySlug(articleSlug);
            if (detail == null) throw new DataNotFoundException();

            Long currentUserId = getCurrentUserId();
            enrichArticleDetailResponse(detail, currentUserId);
            detail.setRelatedArticles(getRelatedArticles(article.getId(), article.getCategory(), 5));
            detail.setSameDateArticles(getSameDateArticles(article.getId(), article.getPublishDate(), 5));

            log.info("Retrieved article detail: {} (views: {})", detail.getTitle(), detail.getViewCount());
            return new DataResponse<>(SUCCESS, "Article detail retrieved successfully",
                    HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting article detail: {}/{}/{}", categorySlug, date, articleSlug, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<NewspaperArticleDetailResponse> getArticleById(Long id) {
        try {
            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailById(id);
            if (detail == null) throw new DataNotFoundException();
            Long currentUserId = getCurrentUserId();
            enrichArticleDetailResponse(detail, currentUserId);
            log.info("Retrieved article by ID: {} ({})", id, detail.getTitle());
            return new DataResponse<>(SUCCESS, "Article retrieved successfully",
                    HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting article by ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DatatableResponse<NewspaperArticleResponse> searchArticles(
            NewspaperSearchCriteria criteria, int page, int limit) {
        try {
            if (criteria.getSearchQuery() == null || criteria.getSearchQuery().trim().isEmpty()) {
                throw new IllegalArgumentException("Search query is required");
            }
            int offset = (page - 1) * limit;
            List<NewspaperArticleResponse> articles = newspaperMapper.searchArticles(criteria, offset, limit);
            int totalCount = newspaperMapper.countSearchArticles(criteria);
            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Search completed successfully",
                    HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error searching articles: {}", criteria.getSearchQuery(), e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<NewspaperArticleResponse> getArticlesOnThisDay(
            int month, int day, int page, int limit) {
        try {
            int offset = (page - 1) * limit;
            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesOnThisDay(month, day, offset, limit);
            int totalCount = newspaperMapper.countArticlesOnThisDay(month, day);
            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Historical articles retrieved successfully",
                    HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error getting articles on this day: {}/{}", month, day, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<NewspaperAnalyticsResponse> getAnalyticsOverview(LocalDate dateFrom, LocalDate dateTo) {
        try {
            if (dateFrom == null) dateFrom = LocalDate.now().minusDays(30);
            if (dateTo == null) dateTo = LocalDate.now();
            NewspaperAnalyticsResponse analytics = newspaperMapper.getAnalyticsOverview(dateFrom, dateTo);
            return new DataResponse<>(SUCCESS, "Analytics retrieved successfully",
                    HttpStatus.OK.value(), analytics);
        } catch (Exception e) {
            log.error("Error getting analytics overview", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<NewspaperArticleResponse>> getTrendingArticles(int days, int limit) {
        try {
            List<NewspaperArticleResponse> trending = newspaperMapper.getTrendingArticles(days, limit);
            Long currentUserId = getCurrentUserId();
            trending.forEach(article -> enrichArticleResponse(article, currentUserId));
            return new DataResponse<>(SUCCESS, "Trending articles retrieved successfully",
                    HttpStatus.OK.value(), trending);
        } catch (Exception e) {
            log.error("Error getting trending articles", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> createArticle(CreateArticleRequest request) {
        try {
            validateCategory(request.getCategory());
            if (newspaperMapper.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException("Article with this slug already exists");
            }
            Long sourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            String plainText = convertHtmlToPlainText(request.getHtmlContent());
            int wordCount = calculateWordCount(plainText);

            NewspaperArticle article = NewspaperArticle.builder()
                    .sourceId(sourceId)
                    .slug(request.getSlug())
                    .category(request.getCategory())
                    .publishDate(request.getPublishDate())
                    .title(request.getTitle())
                    .subtitle(request.getSubtitle())
                    .content(plainText)
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating article", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> updateArticle(Long id, UpdateArticleRequest request) {
        try {
            NewspaperArticle existing = newspaperMapper.findById(id);
            if (existing == null) throw new DataNotFoundException();

            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                validateCategory(request.getCategory());
            }

            String htmlContent = request.getHtmlContent();
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                htmlContent = existing.getHtmlContent();
            }

            String plainText = convertHtmlToPlainText(htmlContent);
            int wordCount = calculateWordCount(plainText);

            Long sourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            if (sourceId != null) {
                existing.setSourceId(sourceId);
            }

            existing.setTitle(request.getTitle());
            existing.setSubtitle(request.getSubtitle());
            existing.setContent(plainText);
            existing.setHtmlContent(htmlContent);
            existing.setWordCount(wordCount);
            existing.setAuthor(request.getAuthor());
            existing.setPageNumber(request.getPageNumber());
            if (request.getImportance() != null && !request.getImportance().isBlank())
                existing.setImportance(request.getImportance());
            if (request.getImageUrl() != null)
                existing.setImageUrl(request.getImageUrl());
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty())
                existing.setCategory(request.getCategory());
            if (request.getPublishDate() != null)
                existing.setPublishDate(request.getPublishDate());

            if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
                String newSlug = request.getSlug().trim();
                if (!newSlug.equals(existing.getSlug())) {
                    if (newspaperMapper.existsBySlugExcluding(newSlug, id)) {
                        throw new IllegalArgumentException("Slug sudah digunakan: " + newSlug);
                    }
                    log.info("Slug updated for article {}: {} -> {}", id, existing.getSlug(), newSlug);
                    existing.setSlug(newSlug);
                }
            }

            newspaperMapper.updateArticle(existing);

            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySlug(existing.getSlug());
            log.info("Article updated: {} (ID: {}, slug: {})", existing.getTitle(), id, existing.getSlug());
            return new DataResponse<>(SUCCESS, "Article updated successfully",
                    HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating article ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteArticle(Long id) {
        try {
            NewspaperArticle existing = newspaperMapper.findById(id);
            if (existing == null) throw new DataNotFoundException();
            newspaperMapper.softDeleteArticle(id);
            log.info("Article soft-deleted: ID {}", id);
            return new DataResponse<>(SUCCESS, "Article deleted successfully",
                    HttpStatus.OK.value(), null);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting article ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    private void trackArticleView(NewspaperArticle article, HttpServletRequest request) {
        try {
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String viewerHash = HashUtil.generateViewerHash(article.getSlug(), userId, ipAddress, userAgent);

            if (!newspaperMapper.hasViewByHash(viewerHash, "view")) {
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
            log.warn("View tracking failed for article {}: {}", article.getId(), e.getMessage());
        }
    }

    private Long resolveSourceId(Long sourceId, String sourceName) {
        if (sourceId != null) return sourceId;
        if (sourceName == null || sourceName.trim().isEmpty()) return null;
        String name = sourceName.trim();
        Long existing = newspaperMapper.findSourceIdByName(name);
        if (existing != null) return existing;
        String slug = generateSlug(name);
        NewspaperSource newSource = NewspaperSource.builder()
                .name(name).slug(slug).isActive(true).build();
        newspaperMapper.insertSource(newSource);
        log.info("New newspaper source created: {} (slug: {})", name, slug);
        return newSource.getId();
    }

    private String generateSlug(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        return text.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String convertHtmlToPlainText(String html) {
        if (html == null || html.trim().isEmpty()) return "";
        String text = html.replaceAll("<[^>]*>", "");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
        return text.trim().replaceAll("\\s+", " ");
    }

    private void enrichArticleResponse(NewspaperArticleResponse article, Long userId) {
        article.setCategoryName(CATEGORY_NAMES.getOrDefault(article.getCategory(), article.getCategory()));
        article.setDateFormatted(article.getPublishDate().format(DATE_FORMATTER));
        article.setIsSaved(false);
        article.setMyRating(null);
        if (userId != null) {
            try {
                article.setIsSaved(newspaperMapper.isArticleSavedByUser(article.getId(), userId));
            } catch (Exception e) {
                log.warn("isArticleSavedByUser failed for {}: {}", article.getId(), e.getMessage());
            }
            try {
                ArticleRating rating = articleRatingMapper.findByUserAndArticle(userId, article.getId());
                article.setMyRating(rating != null ? rating.getRating() : null);
            } catch (Exception e) {
                log.warn("articleRating failed for {}: {}", article.getId(), e.getMessage());
            }
        }
    }

    private void enrichArticleDetailResponse(NewspaperArticleDetailResponse detail, Long userId) {
        detail.setCategoryName(CATEGORY_NAMES.getOrDefault(detail.getCategory(), detail.getCategory()));
        detail.setDateFormatted(detail.getPublishDate().format(DATE_FORMATTER));
        detail.setIsSaved(false);
        detail.setMyRating(null);
        detail.setHasReviewed(false);

        if (detail.getSourceId() != null) {
            NewspaperSourceResponse sourceObj = new NewspaperSourceResponse();
            sourceObj.setId(detail.getSourceId());
            sourceObj.setName(detail.getSourceName() != null ? detail.getSourceName() : UNKNOWN);
            sourceObj.setLocation(detail.getSourceLocation());
            sourceObj.setDescription(detail.getSourceDescription());
            detail.setSource(sourceObj);
        }

        if (userId != null) {
            try {
                detail.setIsSaved(newspaperMapper.isArticleSavedByUser(detail.getId(), userId));
            } catch (Exception e) {
                log.warn("isArticleSavedByUser failed for {}: {}", detail.getId(), e.getMessage());
            }
            try {
                ArticleRating rating = articleRatingMapper.findByUserAndArticle(userId, detail.getId());
                detail.setMyRating(rating != null ? rating.getRating() : null);
            } catch (Exception e) {
                log.warn("articleRating failed for {}: {}", detail.getId(), e.getMessage());
            }
            try {
                detail.setHasReviewed(newspaperMapper.hasUserReviewedArticle(userId, detail.getId()));
            } catch (Exception e) {
                log.warn("hasUserReviewedArticle failed for {}: {}", detail.getId(), e.getMessage());
            }
        }
    }

    private List<NewspaperArticleResponse> getRelatedArticles(Long articleId, String category, int limit) {
        try {
            return newspaperMapper.getRelatedArticles(articleId, category, limit);
        } catch (Exception e) {
            log.error("Error getting related articles", e);
            return Collections.emptyList();
        }
    }

    private List<NewspaperArticleResponse> getSameDateArticles(Long articleId, LocalDate date, int limit) {
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
            return null;
        }
    }

    private void validateCategory(String categorySlug) {
        if (!CATEGORY_NAMES.containsKey(categorySlug)) {
            throw new IllegalArgumentException("Invalid category: " + categorySlug);
        }
    }

    private int calculateWordCount(String content) {
        if (content == null || content.trim().isEmpty()) return 0;
        return content.trim().split("\\s+").length;
    }

    private String getCategoryIcon(String category) {
        return switch (category) {
            case "nasional" -> "🇮🇩";
            case "internasional" -> "🌏";
            case "daerah" -> "📍";
            case "politik" -> "🏛️";
            case "hukum" -> "⚖️";
            case "pemerintahan" -> "🏢";
            case "ekonomi" -> "💰";
            case "bisnis" -> "📈";
            case "pertanian" -> "🌾";
            case "sosial" -> "👥";
            case "pendidikan" -> "📚";
            case "kesehatan" -> "🏥";
            case "agama" -> "🕌";
            case "lingkungan" -> "🌿";
            case "teknologi" -> "💻";
            case "sains" -> "🔬";
            case "budaya" -> "🎭";
            case "hiburan" -> "🎬";
            case "olahraga" -> "⚽";
            case "gaya-hidup" -> "✨";
            case "kuliner" -> "🍜";
            case "wisata" -> "✈️";
            case "opini" -> "✍️";
            case "sastra" -> "📖";
            case "cerita-bersambung" -> "📜";
            case "iklan" -> "📢";
            default -> "📰";
        };
    }

    private String getCategoryDescription(String category) {
        return switch (category) {
            case "nasional" -> "Berita nasional dalam negeri";
            case "internasional" -> "Berita mancanegara dan dunia";
            case "daerah" -> "Berita daerah dan lokal";
            case "politik" -> "Berita politik dan pemerintahan";
            case "hukum" -> "Berita hukum dan kriminal";
            case "pemerintahan" -> "Berita kebijakan pemerintah";
            case "ekonomi" -> "Berita ekonomi makro";
            case "bisnis" -> "Berita bisnis dan keuangan";
            case "pertanian" -> "Berita pertanian dan pangan";
            case "sosial" -> "Berita sosial kemasyarakatan";
            case "pendidikan" -> "Berita pendidikan dan akademis";
            case "kesehatan" -> "Berita kesehatan dan medis";
            case "agama" -> "Berita keagamaan";
            case "lingkungan" -> "Berita lingkungan dan alam";
            case "teknologi" -> "Berita teknologi dan inovasi";
            case "sains" -> "Berita sains dan iptek";
            case "budaya" -> "Berita seni dan budaya";
            case "hiburan" -> "Berita hiburan dan gaya hidup";
            case "olahraga" -> "Berita olahraga";
            case "gaya-hidup" -> "Gaya hidup dan lifestyle";
            case "kuliner" -> "Berita kuliner dan makanan";
            case "wisata" -> "Berita wisata dan perjalanan";
            case "opini" -> "Kolom opini dan editorial";
            case "sastra" -> "Sastra, puisi, dan cerita";
            case "cerita-bersambung" -> "Cerita bersambung dan serial";
            case "iklan" -> "Iklan dan pengumuman";
            default -> "Artikel berita umum";
        };
    }
}