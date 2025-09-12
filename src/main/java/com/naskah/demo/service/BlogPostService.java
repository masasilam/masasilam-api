package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BlogPostService {

    // Blog Post CRUD Operations
    DatatableResponse<BlogPostResponse> getBlogPosts(int page, int limit, String sortField, String sortOrder,
                                                     String status, String category, String tag, String search, Long authorId);

    DataResponse<BlogPostDetailResponse> getBlogPostBySlug(String slug);

    DataResponse<BlogPostResponse> createBlogPost(CreateBlogPostRequest request, List<MultipartFile> images);

    DataResponse<BlogPostResponse> updateBlogPost(Long id, UpdateBlogPostRequest request, List<MultipartFile> images);

    DataResponse<String> deleteBlogPost(Long id);

    // Blog Post Interactions
    DataResponse<BlogPostLikeResponse> toggleLike(Long blogPostId);

    // Comment Operations
    DataResponse<BlogCommentResponse> addComment(Long blogPostId, CreateBlogCommentRequest request);

    DatatableResponse<BlogCommentResponse> getBlogPostComments(Long blogPostId, int page, int limit,
                                                               String sortField, String sortOrder);

    DataResponse<BlogCommentResponse> updateComment(Long commentId, UpdateBlogCommentRequest request);

    DataResponse<String> deleteComment(Long commentId);

    // Blog Post Discovery
    DatatableResponse<BlogPostResponse> getTrendingBlogPosts(int page, int limit, int days);

    DatatableResponse<BlogPostResponse> getBlogPostsByCategory(String categorySlug, int page, int limit,
                                                               String sortField, String sortOrder);

    DatatableResponse<BlogPostResponse> getBlogPostsByTag(String tagSlug, int page, int limit,
                                                          String sortField, String sortOrder);

    DatatableResponse<BlogPostResponse> getMyBlogPosts(int page, int limit, String sortField,
                                                       String sortOrder, String status);

    DatatableResponse<BlogPostResponse> searchBlogPosts(String query, int page, int limit,
                                                        String sortField, String sortOrder,
                                                        String category, String tag);

    // Statistics and Analytics
    DataResponse<BlogStatsResponse> getBlogStats();
}