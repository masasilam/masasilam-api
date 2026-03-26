package com.naskah.demo.controller.newspaper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.newspaper.NewspaperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/newspapers")
@RequiredArgsConstructor
public class NewspaperController {

    private final NewspaperService newspaperService;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        DataResponse<NewspaperArticleDetailResponse> response = newspaperService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── READ by ID (untuk form edit dashboard) ────────────────────────────────

    @GetMapping("/articles/{id}")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> getArticleById(@PathVariable Long id) {
        DataResponse<NewspaperArticleDetailResponse> response = newspaperService.getArticleById(id);
        return ResponseEntity.ok(response);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/articles/{id}")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> updateArticle(@PathVariable Long id,
                                                                                      @Valid @RequestBody UpdateArticleRequest request) {
        DataResponse<NewspaperArticleDetailResponse> response = newspaperService.updateArticle(id, request);
        return ResponseEntity.ok(response);
    }

    // ── DELETE (soft delete) ──────────────────────────────────────────────────

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<DataResponse<Void>> deleteArticle(@PathVariable Long id) {
        DataResponse<Void> response = newspaperService.deleteArticle(id);
        return ResponseEntity.ok(response);
    }

    // ── METADATA ──────────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<DataResponse<List<NewspaperCategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(newspaperService.getAllCategories());
    }

    @GetMapping("/sources")
    public ResponseEntity<DatatableResponse<NewspaperSourceResponse>> getAllSources(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                    @RequestParam(defaultValue = "20") @Min(1) int limit,
                                                                                    @RequestParam(required = false) String search) {
        return ResponseEntity.ok(newspaperService.getAllSources(page, limit, search));
    }

    @GetMapping("/stats")
    public ResponseEntity<DataResponse<NewspaperStatsResponse>> getStats() {
        return ResponseEntity.ok(newspaperService.getOverallStats());
    }

    // ── BROWSE ────────────────────────────────────────────────────────────────

    @GetMapping("/categories/{categorySlug}")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getArticlesByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String importance) {

        NewspaperSearchCriteria criteria = NewspaperSearchCriteria.builder()
                .category(categorySlug).dateFrom(dateFrom).dateTo(dateTo)
                .source(source).importance(importance).build();
        return ResponseEntity.ok(
                newspaperService.getArticlesByCategory(categorySlug, page, limit, sortBy, sortOrder, criteria));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getArticlesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "importance") String sortBy,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(newspaperService.getArticlesByDate(date, page, limit, sortBy, category));
    }

    @GetMapping("/{categorySlug}/{date}")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getArticlesByCategoryAndDate(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "importance") String sortBy,
            @RequestParam(required = false) String source) {
        return ResponseEntity.ok(
                newspaperService.getArticlesByCategoryAndDate(categorySlug, date, page, limit, sortBy, source));
    }

    // ── ARTICLE DETAIL ────────────────────────────────────────────────────────

    @GetMapping("/{categorySlug}/{date}/{articleSlug}")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> getArticleDetail(@PathVariable String categorySlug,
                                                                                         @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                                         @PathVariable String articleSlug,
                                                                                         HttpServletRequest request) {
        return ResponseEntity.ok(newspaperService.getArticleDetail(categorySlug, date, articleSlug, request));
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> searchArticles(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String importance) {

        NewspaperSearchCriteria criteria = NewspaperSearchCriteria.builder()
                .searchQuery(q).category(category).dateFrom(dateFrom)
                .dateTo(dateTo).source(source).importance(importance).build();
        return ResponseEntity.ok(newspaperService.searchArticles(criteria, page, limit));
    }

    // ── ON THIS DAY ───────────────────────────────────────────────────────────

    @GetMapping("/on-this-day")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getOnThisDay(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        LocalDate today = LocalDate.now();
        int targetMonth = month != null ? month : today.getMonthValue();
        int targetDay   = day   != null ? day   : today.getDayOfMonth();
        return ResponseEntity.ok(newspaperService.getArticlesOnThisDay(targetMonth, targetDay, page, limit));
    }

    // ── ANALYTICS ─────────────────────────────────────────────────────────────

    @GetMapping("/analytics/overview")
    public ResponseEntity<DataResponse<NewspaperAnalyticsResponse>> getAnalyticsOverview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(newspaperService.getAnalyticsOverview(dateFrom, dateTo));
    }

    @GetMapping("/trending")
    public ResponseEntity<DataResponse<List<NewspaperArticleResponse>>> getTrendingArticles(@RequestParam(defaultValue = "7") int days,
                                                                                            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(newspaperService.getTrendingArticles(days, limit));
    }
}