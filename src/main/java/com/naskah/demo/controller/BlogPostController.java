package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.BlogPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    // Get all blog posts with pagination and filters
    @GetMapping
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long authorId) {
        DatatableResponse<BlogPostResponse> response = blogPostService.getBlogPosts(
                page, limit, sortField, sortOrder, status, category, tag, search, authorId);
        return ResponseEntity.ok(response);
    }

    // Get single blog post by slug
    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<BlogPostDetailResponse>> getBlogPostBySlug(
            @PathVariable String slug) {
        DataResponse<BlogPostDetailResponse> response = blogPostService.getBlogPostBySlug(slug);
        return ResponseEntity.ok(response);
    }

    // Create new blog post
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BlogPostResponse>> createBlogPost(
            @RequestPart("blogPost") @Valid CreateBlogPostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        DataResponse<BlogPostResponse> response = blogPostService.createBlogPost(request, images);
        return ResponseEntity.ok(response);
    }

    // Update blog post
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BlogPostResponse>> updateBlogPost(
            @PathVariable Long id,
            @RequestPart("blogPost") @Valid UpdateBlogPostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        DataResponse<BlogPostResponse> response = blogPostService.updateBlogPost(id, request, images);
        return ResponseEntity.ok(response);
    }

    // Delete blog post
    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteBlogPost(@PathVariable Long id) {
        DataResponse<String> response = blogPostService.deleteBlogPost(id);
        return ResponseEntity.ok(response);
    }

    // Like/Unlike blog post
    @PostMapping("/{id}/like")
    public ResponseEntity<DataResponse<BlogPostLikeResponse>> toggleLike(@PathVariable Long id) {
        DataResponse<BlogPostLikeResponse> response = blogPostService.toggleLike(id);
        return ResponseEntity.ok(response);
    }

    // Add comment to blog post
    @PostMapping("/{id}/comments")
    public ResponseEntity<DataResponse<BlogCommentResponse>> addComment(
            @PathVariable Long id,
            @RequestBody @Valid CreateBlogCommentRequest request) {
        DataResponse<BlogCommentResponse> response = blogPostService.addComment(id, request);
        return ResponseEntity.ok(response);
    }

    // Get comments for blog post
    @GetMapping("/{id}/comments")
    public ResponseEntity<DatatableResponse<BlogCommentResponse>> getBlogPostComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<BlogCommentResponse> response = blogPostService.getBlogPostComments(
                id, page, limit, sortField, sortOrder);
        return ResponseEntity.ok(response);
    }

    // Update comment
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<BlogCommentResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateBlogCommentRequest request) {
        DataResponse<BlogCommentResponse> response = blogPostService.updateComment(commentId, request);
        return ResponseEntity.ok(response);
    }

    // Delete comment
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<String>> deleteComment(@PathVariable Long commentId) {
        DataResponse<String> response = blogPostService.deleteComment(commentId);
        return ResponseEntity.ok(response);
    }

    // Get trending blog posts
    @GetMapping("/trending")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getTrendingBlogPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "7", required = false) int days) {
        DatatableResponse<BlogPostResponse> response = blogPostService.getTrendingBlogPosts(page, limit, days);
        return ResponseEntity.ok(response);
    }

    // Get blog posts by category
    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPostsByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<BlogPostResponse> response = blogPostService.getBlogPostsByCategory(
                categorySlug, page, limit, sortField, sortOrder);
        return ResponseEntity.ok(response);
    }

    // Get blog posts by tag
    @GetMapping("/tag/{tagSlug}")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPostsByTag(
            @PathVariable String tagSlug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<BlogPostResponse> response = blogPostService.getBlogPostsByTag(
                tagSlug, page, limit, sortField, sortOrder);
        return ResponseEntity.ok(response);
    }

    // Get user's blog posts
    @GetMapping("/my-posts")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getMyBlogPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String status) {
        DatatableResponse<BlogPostResponse> response = blogPostService.getMyBlogPosts(
                page, limit, sortField, sortOrder, status);
        return ResponseEntity.ok(response);
    }

    // Search blog posts
    @GetMapping("/search")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> searchBlogPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "createdAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        DatatableResponse<BlogPostResponse> response = blogPostService.searchBlogPosts(
                query, page, limit, sortField, sortOrder, category, tag);
        return ResponseEntity.ok(response);
    }

    // Get blog statistics for dashboard
    @GetMapping("/stats")
    public ResponseEntity<DataResponse<BlogStatsResponse>> getBlogStats() {
        DataResponse<BlogStatsResponse> response = blogPostService.getBlogStats();
        return ResponseEntity.ok(response);
    }
}