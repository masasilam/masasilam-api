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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/newspapers")
@RequiredArgsConstructor
public class NewspaperController {

    private final NewspaperService newspaperService;

    @PostMapping("/articles")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> createArticle(
            @RequestParam("sourceId") Long sourceId,
            @RequestParam("slug") String slug,
            @RequestParam("category") String category,
            @RequestParam("publishDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
            @RequestParam("title") String title,
            @RequestParam("htmlFile") MultipartFile htmlFile,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String importance,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) Long parentArticleId,
            @RequestParam(required = false) Integer articleLevel) throws IOException {

        // Validate file
        if (htmlFile.isEmpty()) {
            throw new IllegalArgumentException("HTML file is required");
        }

        // Validate file type (optional but recommended)
        String contentType = htmlFile.getContentType();
        if (contentType != null && !contentType.equals("text/html") && !contentType.equals("text/plain")) {
            throw new IllegalArgumentException("Only HTML or text files are allowed");
        }

        // Read HTML content from file
        String htmlContent = new String(htmlFile.getBytes(), StandardCharsets.UTF_8);

        // Build request object
        CreateArticleRequest request = CreateArticleRequest.builder()
                .sourceId(sourceId)
                .slug(slug)
                .category(category)
                .publishDate(publishDate)
                .title(title)
                .htmlContent(htmlContent)
                .author(author)
                .pageNumber(pageNumber)
                .importance(importance)
                .imageUrl(imageUrl)
                .parentArticleId(parentArticleId)
                .articleLevel(articleLevel)
                .build();

        DataResponse<NewspaperArticleDetailResponse> response = newspaperService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArticleRequest request) {
        DataResponse<NewspaperArticleDetailResponse> response = newspaperService.updateArticle(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<DataResponse<List<NewspaperCategoryResponse>>> getAllCategories() {
        DataResponse<List<NewspaperCategoryResponse>> response = newspaperService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sources")
    public ResponseEntity<DatatableResponse<NewspaperSourceResponse>> getAllSources(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String search) {
        DatatableResponse<NewspaperSourceResponse> response = newspaperService.getAllSources(page, limit, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<DataResponse<NewspaperStatsResponse>> getStats() {
        DataResponse<NewspaperStatsResponse> response = newspaperService.getOverallStats();
        return ResponseEntity.ok(response);
    }

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
                .category(categorySlug)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .importance(importance)
                .build();

        DatatableResponse<NewspaperArticleResponse> response =
                newspaperService.getArticlesByCategory(categorySlug, page, limit, sortBy, sortOrder, criteria);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getArticlesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "importance") String sortBy,
            @RequestParam(required = false) String category) {

        DatatableResponse<NewspaperArticleResponse> response =
                newspaperService.getArticlesByDate(date, page, limit, sortBy, category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categorySlug}/{date}")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getArticlesByCategoryAndDate(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "importance") String sortBy,
            @RequestParam(required = false) String source) {

        DatatableResponse<NewspaperArticleResponse> response =
                newspaperService.getArticlesByCategoryAndDate(categorySlug, date, page, limit, sortBy, source);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categorySlug}/{date}/{articleSlug}")
    public ResponseEntity<DataResponse<NewspaperArticleDetailResponse>> getArticleDetail(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            HttpServletRequest request) {

        DataResponse<NewspaperArticleDetailResponse> response =
                newspaperService.getArticleDetail(categorySlug, date, articleSlug, request);
        return ResponseEntity.ok(response);
    }

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
                .searchQuery(q)
                .category(category)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .importance(importance)
                .build();

        DatatableResponse<NewspaperArticleResponse> response =
                newspaperService.searchArticles(criteria, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/on-this-day")
    public ResponseEntity<DatatableResponse<NewspaperArticleResponse>> getOnThisDay(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {

        LocalDate today = LocalDate.now();
        int targetMonth = month != null ? month : today.getMonthValue();
        int targetDay = day != null ? day : today.getDayOfMonth();

        DatatableResponse<NewspaperArticleResponse> response =
                newspaperService.getArticlesOnThisDay(targetMonth, targetDay, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<DataResponse<NewspaperAnalyticsResponse>> getAnalyticsOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        DataResponse<NewspaperAnalyticsResponse> response =
                newspaperService.getAnalyticsOverview(dateFrom, dateTo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    public ResponseEntity<DataResponse<List<NewspaperArticleResponse>>> getTrendingArticles(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {

        DataResponse<List<NewspaperArticleResponse>> response =
                newspaperService.getTrendingArticles(days, limit);
        return ResponseEntity.ok(response);
    }
}