package com.masasilam.app.service.newspaper.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.author.AuthorMapper;
import com.masasilam.app.mapper.author.ContributorMapper;
import com.masasilam.app.mapper.book.GenreMapper;
import com.masasilam.app.mapper.newspaper.ArticleRatingMapper;
import com.masasilam.app.mapper.newspaper.NewspaperMapper;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.ContributorMetadata;
import com.masasilam.app.model.dto.newspaper.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.Author;
import com.masasilam.app.model.entity.Contributor;
import com.masasilam.app.model.entity.Genre;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.newspaper.*;
import com.masasilam.app.service.newspaper.NewspaperService;
import com.masasilam.app.util.HashUtil;
import com.masasilam.app.util.IPUtil;
import com.masasilam.app.util.file.FileUtil;
import com.masasilam.app.util.interceptor.HeaderHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewspaperServiceImpl implements NewspaperService {
    private final NewspaperMapper newspaperMapper;
    private final ArticleRatingMapper articleRatingMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;
    private final AuthorMapper authorMapper;
    private final ContributorMapper contributorMapper;
    private final GenreMapper genreMapper;
    private final FileUtil fileUtil;

    private static final String SUCCESS = "Success";
    private static final String UNKNOWN = "Unknown";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    @Override
    public DataResponse<List<NewspaperCategoryResponse>> getAllCategories() {
        try {
            List<NewspaperCategoryResponse> categories = newspaperMapper.getAllCategories();
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
            validateGenreSlug(categorySlug);
            int offset = (page - 1) * limit;
            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesByCategory(categorySlug, offset, limit, sortBy, sortOrder, criteria);
            int totalCount = newspaperMapper.countArticlesByCategory(categorySlug, criteria);
            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully", HttpStatus.OK.value(), pageData);
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
            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesByDate(date, offset, limit, sortBy, category);
            int totalCount = newspaperMapper.countArticlesByDate(date, category);
            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));
            PageDataResponse<NewspaperArticleResponse> pageData = new PageDataResponse<>(page, limit, totalCount, articles);
            return new DatatableResponse<>(SUCCESS, "Articles retrieved successfully", HttpStatus.OK.value(), pageData);
        } catch (Exception e) {
            log.error("Error getting articles by date: {}", date, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> getArticleDetail(
            String sourceSlug, String articleSlug, HttpServletRequest request) {
        try {
            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailBySourceAndSlug(sourceSlug, articleSlug);
            if (detail == null) throw new DataNotFoundException();

            trackArticleView(detail.getId(), detail.getSlug(), request);

            Long currentUserId = getCurrentUserId();
            enrichArticleDetailResponse(detail, currentUserId);
            detail.setRelatedArticles(getRelatedArticles(detail.getId(), primaryGenreSlug(detail)));
            detail.setSameDateArticles(getSameDateArticles(detail.getId(), detail.getPublishDate()));

            log.info("Retrieved newspaper detail: {} (views: {})", detail.getTitle(), detail.getViewCount());
            return new DataResponse<>(SUCCESS, "Article detail retrieved successfully", HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting newspaper detail: {}/{}", sourceSlug, articleSlug, e);
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
            log.info("Retrieved newspaper by ID: {} ({})", id, detail.getTitle());
            return new DataResponse<>(SUCCESS, "Article retrieved successfully",
                    HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting newspaper by ID: {}", id, e);
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
            Long sourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            if (sourceId == null) {
                throw new IllegalArgumentException("Sumber koran wajib diisi");
            }
            if (newspaperMapper.existsBySlugForSource(sourceId, request.getSlug())) {
                throw new IllegalArgumentException("Artikel dengan slug ini sudah ada untuk sumber tersebut");
            }
            String plainText = convertHtmlToPlainText(request.getHtmlContent());
            int wordCount = calculateWordCount(plainText);

            NewspaperArticle article = NewspaperArticle.builder()
                    .sourceId(sourceId)
                    .slug(request.getSlug())
                    .publishDate(request.getPublishDate())
                    .title(request.getTitle())
                    .subtitle(request.getSubtitle())
                    .content(plainText)
                    .htmlContent(request.getHtmlContent())
                    .wordCount(wordCount)
                    .pageNumber(request.getPageNumber())
                    .importance(request.getImportance() != null ? request.getImportance() : "medium")
                    .imageUrl(request.getImageUrl())
                    .parentArticleId(request.getParentArticleId())
                    .articleLevel(request.getArticleLevel() != null ? request.getArticleLevel() : 0)
                    .isActive(true)
                    .isFeatured(false)
                    .build();

            newspaperMapper.insertArticle(article);

            authorProcessing(request.getAuthorNames(), article.getId());
            genreProcessing(request.getGenreNames(), article.getId());
            contributorProcessing(request.getContributors(), article.getId());

            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailById(article.getId());
            log.info("Article created: {} (ID: {})", article.getTitle(), article.getId());
            return new DataResponse<>(SUCCESS, "Article created successfully", HttpStatus.CREATED.value(), detail);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating newspaper", e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperArticleDetailResponse> updateArticle(Long id, UpdateArticleRequest request) {
        try {
            NewspaperArticle existing = newspaperMapper.findById(id);
            if (existing == null) throw new DataNotFoundException();

            String htmlContent = request.getHtmlContent();
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                htmlContent = existing.getHtmlContent();
            }

            String plainText = convertHtmlToPlainText(htmlContent);
            int wordCount = calculateWordCount(plainText);

            Long resolvedSourceId = existing.getSourceId();
            Long candidateSourceId = resolveSourceId(request.getSourceId(), request.getSourceName());
            if (candidateSourceId != null) {
                resolvedSourceId = candidateSourceId;
                existing.setSourceId(candidateSourceId);
            }

            existing.setTitle(request.getTitle());
            existing.setSubtitle(request.getSubtitle());
            existing.setContent(plainText);
            existing.setHtmlContent(htmlContent);
            existing.setWordCount(wordCount);
            existing.setPageNumber(request.getPageNumber());
            if (request.getImportance() != null && !request.getImportance().isBlank())
                existing.setImportance(request.getImportance());
            if (request.getImageUrl() != null)
                existing.setImageUrl(request.getImageUrl());
            if (request.getPublishDate() != null)
                existing.setPublishDate(request.getPublishDate());

            if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
                String newSlug = request.getSlug().trim();
                if (!newSlug.equals(existing.getSlug())) {
                    if (newspaperMapper.existsBySlugForSourceExcluding(resolvedSourceId, newSlug, id)) {
                        throw new IllegalArgumentException("Slug sudah digunakan untuk sumber ini: " + newSlug);
                    }
                    log.info("Slug updated for newspaper {}: {} -> {}", id, existing.getSlug(), newSlug);
                    existing.setSlug(newSlug);
                }
            }

            newspaperMapper.updateArticle(existing);

            if (request.getAuthorNames() != null) {
                authorProcessing(request.getAuthorNames(), id);
            }
            if (request.getGenreNames() != null) {
                newspaperMapper.deleteArticleGenres(id);
                genreProcessing(request.getGenreNames(), id);
            }
            if (request.getContributors() != null) {
                newspaperMapper.deleteArticleContributors(id);
                contributorProcessing(request.getContributors(), id);
            }

            NewspaperArticleDetailResponse detail = newspaperMapper.getArticleDetailById(id);
            log.info("Article updated: {} (ID: {}, slug: {})", existing.getTitle(), id, existing.getSlug());
            return new DataResponse<>(SUCCESS, "Article updated successfully",
                    HttpStatus.OK.value(), detail);
        } catch (DataNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating newspaper ID: {}", id, e);
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
            log.error("Error deleting newspaper ID: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<NewspaperSourceDetailResponse> getSourceDetail(String sourceSlug) {
        try {
            NewspaperSourceDetailResponse source = newspaperMapper.getSourceDetailBySlug(sourceSlug);
            if (source == null) throw new DataNotFoundException();
            source.setYears(newspaperMapper.getSourceAvailableYears(source.getId()));
            return new DataResponse<>(SUCCESS, "Source detail retrieved successfully", HttpStatus.OK.value(), source);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting source detail: {}", sourceSlug, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<NewspaperEditionResponse>> getEditions(String sourceSlug, int year, Integer month, LocalDate dateFrom, LocalDate dateTo) {
        try {
            Long sourceId = newspaperMapper.findSourceIdBySlug(sourceSlug);
            if (sourceId == null) throw new DataNotFoundException();
            List<NewspaperEditionResponse> editions = newspaperMapper.getEditionsBySourceAndYear(sourceId, year, month, dateFrom, dateTo);
            editions.forEach(e -> e.setDateFormatted(e.getPublishDate().format(DATE_FORMATTER)));
            return new DataResponse<>(SUCCESS, "Editions retrieved successfully", HttpStatus.OK.value(), editions);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting editions: {}/{}", sourceSlug, year, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<NewspaperArticleResponse>> getEditionArticles(String sourceSlug, LocalDate date) {
        try {
            Long sourceId = newspaperMapper.findSourceIdBySlug(sourceSlug);
            if (sourceId == null) throw new DataNotFoundException();
            List<NewspaperArticleResponse> articles = newspaperMapper.getArticlesBySourceAndDate(sourceId, date);
            if (articles.isEmpty()) throw new DataNotFoundException();
            Long currentUserId = getCurrentUserId();
            articles.forEach(a -> enrichArticleResponse(a, currentUserId));
            return new DataResponse<>(SUCCESS, "Edition articles retrieved successfully", HttpStatus.OK.value(), articles);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting edition articles: {}/{}", sourceSlug, date, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public List<NewspaperSourceResponse> getAllSourcesForSitemap() {
        try {
            return newspaperMapper.getAllSourcesForSitemap();
        } catch (Exception e) {
            log.error("Error getting sources for sitemap", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<NewspaperSitemapItemResponse> getArticlesForSitemap() {
        try {
            return newspaperMapper.getArticlesForSitemap();
        } catch (Exception e) {
            log.error("Error getting articles for sitemap", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public DataResponse<NewspaperSourceResponse> updateSource(Long id, UpdateSourceRequest request) {
        try {
            if (newspaperMapper.findSourceById(id) == null) throw new DataNotFoundException();
            newspaperMapper.updateSource(id, request.getName(), request.getDescription(), request.getLocation(), request.getLogoUrl());
            NewspaperSourceResponse updated = newspaperMapper.getSourceById(id);
            return new DataResponse<>(SUCCESS, "Source updated successfully", HttpStatus.OK.value(), updated);
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating source: {}", id, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public DataResponse<List<NewspaperArticleResponse>> getLatestArticles(int limit) {
        try {
            List<NewspaperArticleResponse> articles = newspaperMapper.getLatestArticles(0, limit);
            Long currentUserId = getCurrentUserId();
            articles.forEach(article -> enrichArticleResponse(article, currentUserId));
            return new DataResponse<>(SUCCESS, "Latest articles retrieved successfully", HttpStatus.OK.value(), articles);
        } catch (Exception e) {
            log.error("Error getting latest articles", e);
            throw new InternalServerErrorException();
        }
    }

    private void genreProcessing(List<String> genreNames, Long articleId) {
        if (genreNames == null || genreNames.isEmpty()) return;
        for (String name : genreNames) {
            if (name == null || name.isBlank()) continue;
            Genre genre = genreMapper.findByName(name);
            if (genre == null) {
                genre = new Genre();
                genre.setName(name.trim());
                genre.setSlug(fileUtil.sanitizeFilename(name));
                genre.setDescription("Auto-generated from newspaper article");
                genre.setColorHex("#6B7280");
                genre.setIconName("newspaper");
                genre.setIsFiction(false);
                genre.setCreatedAt(Instant.now());
                genreMapper.insertGenre(genre);
                log.info("Auto-created genre from article: {}", name);
            }
            newspaperMapper.insertArticleGenre(articleId, genre.getId());
        }
    }

    private void authorProcessing(List<String> authorNames, Long articleId) {
        if (authorNames == null || authorNames.isEmpty()) return;

        Set<Long> existingAuthorIds = newspaperMapper.findAuthorsByArticleId(articleId).stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        for (String name : authorNames) {
            if (name == null || name.isBlank()) continue;
            String slug = fileUtil.sanitizeFilename(name);
            Author author = authorMapper.findAuthorBySlug(slug);

            if (author == null) {
                author = new Author();
                author.setName(name.trim());
                author.setSlug(slug);
                author.setTotalBooks(1);
                author.setCreatedAt(LocalDateTime.now());
                author.setUpdatedAt(LocalDateTime.now());
                authorMapper.insertAuthor(author);
                newspaperMapper.insertArticleAuthor(articleId, author.getId());
                log.info("Auto-created author from article: {} (total karya: 1)", name);
                continue;
            }

            if (!existingAuthorIds.contains(author.getId())) {
                author.setTotalBooks((author.getTotalBooks() != null ? author.getTotalBooks() : 0) + 1);
                author.setUpdatedAt(LocalDateTime.now());
                authorMapper.updateAuthor(author);
                newspaperMapper.insertArticleAuthor(articleId, author.getId());
                log.info("Linked existing author '{}' to article, total karya sekarang: {}", name, author.getTotalBooks());
            }
        }
    }

    private void contributorProcessing(List<ContributorMetadata> contributors, Long articleId) {
        if (contributors == null || contributors.isEmpty()) return;
        for (ContributorMetadata c : contributors) {
            if (c.getName() == null || c.getName().isBlank()) continue;
            Contributor contributor = contributorMapper.findByNameAndRole(c.getName(), c.getRole());
            if (contributor == null) {
                contributor = new Contributor();
                contributor.setName(c.getName().trim());
                contributor.setRole(c.getRole());
                String baseSlug = fileUtil.sanitizeFilename(c.getName());
                String finalSlug = contributorMapper.findBySlug(baseSlug) != null
                        ? baseSlug + "-" + c.getRole().toLowerCase().replace(" ", "-")
                        : baseSlug;
                contributor.setSlug(finalSlug);
                contributor.setCreatedAt(LocalDateTime.now());
                contributor.setUpdatedAt(LocalDateTime.now());
                contributorMapper.insertContributor(contributor);
                log.info("Auto-created contributor from article: {} ({})", c.getName(), c.getRole());
            }
            newspaperMapper.insertArticleContributor(articleId, contributor.getId(), c.getRole());
        }
    }

    private void trackArticleView(Long articleId, String slug, HttpServletRequest request) {
        try {
            String ipAddress = IPUtil.getClientIP(request);
            String userAgent = IPUtil.getUserAgent(request);
            Long userId = getCurrentUserId();
            String viewerHash = HashUtil.generateViewerHash(slug, userId, ipAddress, userAgent);

            if (!newspaperMapper.hasViewByHash(viewerHash, "view")) {
                ArticleView view = ArticleView.builder()
                        .articleId(articleId)
                        .userId(userId)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .viewerHash(viewerHash)
                        .actionType("view")
                        .build();
                newspaperMapper.insertArticleView(view);
                newspaperMapper.incrementViewCount(articleId);
                log.debug("New view recorded for newspaper: {}", slug);
            }
        } catch (Exception e) {
            log.warn("View tracking failed for newspaper {}: {}", articleId, e.getMessage());
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

    private String primaryGenreSlug(NewspaperArticleDetailResponse detail) {
        if (detail.getGenreSlugs() == null || detail.getGenreSlugs().isBlank()) return null;
        return detail.getGenreSlugs().split(",")[0].trim();
    }

    private void enrichArticleResponse(NewspaperArticleResponse article, Long userId) {
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
        detail.setDateFormatted(detail.getPublishDate().format(DATE_FORMATTER));
        detail.setIsSaved(false);
        detail.setMyRating(null);
        detail.setHasReviewed(false);

        if (detail.getSourceId() != null) {
            NewspaperSourceResponse sourceObj = new NewspaperSourceResponse();
            sourceObj.setId(detail.getSourceId());
            sourceObj.setName(detail.getSourceName() != null ? detail.getSourceName() : UNKNOWN);
            sourceObj.setSlug(detail.getSourceSlug());
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

    private List<NewspaperArticleResponse> getRelatedArticles(Long articleId, String categorySlug) {
        try {
            if (categorySlug == null) return Collections.emptyList();
            return newspaperMapper.getRelatedArticles(articleId, categorySlug, 5);
        } catch (Exception e) {
            log.error("Error getting related articles", e);
            return Collections.emptyList();
        }
    }

    private List<NewspaperArticleResponse> getSameDateArticles(Long articleId, LocalDate date) {
        try {
            return newspaperMapper.getSameDateArticles(articleId, date, 5);
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

    private void validateGenreSlug(String slug) {
        if (!newspaperMapper.genreSlugExists(slug)) {
            throw new InvalidDataException("Genre tidak ditemukan: " + slug);
        }
    }

    private int calculateWordCount(String content) {
        if (content == null || content.trim().isEmpty()) return 0;
        return content.trim().split("\\s+").length;
    }
}