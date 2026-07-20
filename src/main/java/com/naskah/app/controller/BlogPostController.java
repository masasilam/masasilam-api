package com.naskah.app.controller;

import com.naskah.app.model.dto.request.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.service.BlogPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogPostController {
    private final BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPosts(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                            @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                            @RequestParam(defaultValue = "createdAt") String sortField,
                                                                            @RequestParam(defaultValue = "DESC") String sortOrder,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) String category,
                                                                            @RequestParam(required = false) String tag,
                                                                            @RequestParam(required = false) String search,
                                                                            @RequestParam(required = false) Long authorId) {
        return ResponseEntity.ok(blogPostService.getBlogPosts(page, limit, sortField, sortOrder, status, category, tag, search, authorId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<BlogPostDetailResponse>> getBlogPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogPostService.getBlogPostBySlug(slug));
    }

    @GetMapping("/trending")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getTrendingBlogPosts(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                    @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                    @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(blogPostService.getTrendingBlogPosts(page, limit, days));
    }

    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPostsByCategory(@PathVariable String categorySlug,
                                                                                      @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                      @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                      @RequestParam(defaultValue = "createdAt") String sortField,
                                                                                      @RequestParam(defaultValue = "DESC") String sortOrder) {
        return ResponseEntity.ok(blogPostService.getBlogPostsByCategory(categorySlug, page, limit, sortField, sortOrder));
    }

    @GetMapping("/tag/{tagSlug}")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getBlogPostsByTag(@PathVariable String tagSlug,
                                                                                 @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                 @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                 @RequestParam(defaultValue = "createdAt") String sortField,
                                                                                 @RequestParam(defaultValue = "DESC") String sortOrder) {
        return ResponseEntity.ok(blogPostService.getBlogPostsByTag(tagSlug, page, limit, sortField, sortOrder));
    }

    @GetMapping("/search")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> searchBlogPosts(@RequestParam String query,
                                                                               @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                               @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                               @RequestParam(defaultValue = "createdAt") String sortField,
                                                                               @RequestParam(defaultValue = "DESC") String sortOrder,
                                                                               @RequestParam(required = false) String category,
                                                                               @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(blogPostService.searchBlogPosts(query, page, limit, sortField, sortOrder, category, tag));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<DatatableResponse<BlogCommentResponse>> getBlogPostComments(@PathVariable Long id,
                                                                                      @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                      @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                      @RequestParam(defaultValue = "createdAt") String sortField,
                                                                                      @RequestParam(defaultValue = "DESC") String sortOrder) {
        return ResponseEntity.ok(blogPostService.getBlogPostComments(id, page, limit, sortField, sortOrder));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<DataResponse<BlogPostLikeResponse>> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(blogPostService.toggleLike(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<DataResponse<BlogCommentResponse>> addComment(@PathVariable Long id,
                                                                        @RequestBody @Valid CreateBlogCommentRequest request) {
        return ResponseEntity.ok(blogPostService.addComment(id, request));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<BlogCommentResponse>> updateComment(@PathVariable Long commentId,
                                                                           @RequestBody @Valid UpdateBlogCommentRequest request) {
        return ResponseEntity.ok(blogPostService.updateComment(commentId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<String>> deleteComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(blogPostService.deleteComment(commentId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BlogPostResponse>> createBlogPost(@RequestPart("blogPost") @Valid CreateBlogPostRequest request,
                                                                         @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(blogPostService.createBlogPost(request, images));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BlogPostResponse>> updateBlogPost(@PathVariable Long id,
                                                                         @RequestPart("blogPost") @Valid UpdateBlogPostRequest request,
                                                                         @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(blogPostService.updateBlogPost(id, request, images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteBlogPost(@PathVariable Long id) {
        return ResponseEntity.ok(blogPostService.deleteBlogPost(id));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Map<String, String>>> uploadInlineImage(@RequestPart("image") MultipartFile image,
                                                                               @RequestParam(value = "postId", required = false) Long postId) {
        return ResponseEntity.ok(blogPostService.uploadInlineImage(image, postId));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<DatatableResponse<BlogPostResponse>> getMyBlogPosts(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                              @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                              @RequestParam(defaultValue = "createdAt") String sortField,
                                                                              @RequestParam(defaultValue = "DESC") String sortOrder,
                                                                              @RequestParam(required = false) String status) {
        return ResponseEntity.ok(blogPostService.getMyBlogPosts(page, limit, sortField, sortOrder, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<DataResponse<BlogStatsResponse>> getBlogStats() {
        return ResponseEntity.ok(blogPostService.getBlogStats());
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<DataResponse<BlogPostDetailResponse>> getBlogPostByIdForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(blogPostService.getBlogPostByIdForAdmin(id));
    }
}