package com.masasilam.app.service.social;

import com.masasilam.app.model.dto.request.CreateBlogCommentRequest;
import com.masasilam.app.model.dto.request.CreateBlogPostRequest;
import com.masasilam.app.model.dto.request.UpdateBlogCommentRequest;
import com.masasilam.app.model.dto.request.UpdateBlogPostRequest;
import com.masasilam.app.model.dto.response.BlogCommentResponse;
import com.masasilam.app.model.dto.response.BlogPostDetailResponse;
import com.masasilam.app.model.dto.response.BlogPostLikeResponse;
import com.masasilam.app.model.dto.response.BlogPostResponse;
import com.masasilam.app.model.dto.response.BlogStatsResponse;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BlogPostService {
    DatatableResponse<BlogPostResponse> getBlogPosts(int page, int limit, String sortField, String sortOrder, String status, String category, String tag, String search, Long authorId);
    DataResponse<BlogPostDetailResponse> getBlogPostBySlug(String slug);
    DataResponse<BlogPostResponse> createBlogPost(CreateBlogPostRequest request, List<MultipartFile> images);
    DataResponse<BlogPostResponse> updateBlogPost(Long id, UpdateBlogPostRequest request, List<MultipartFile> images);
    DataResponse<String> deleteBlogPost(Long id);
    DataResponse<BlogPostLikeResponse> toggleLike(Long blogPostId);
    DataResponse<BlogCommentResponse> addComment(Long blogPostId, CreateBlogCommentRequest request);
    DatatableResponse<BlogCommentResponse> getBlogPostComments(Long blogPostId, int page, int limit, String sortField, String sortOrder);
    DataResponse<BlogCommentResponse> updateComment(Long commentId, UpdateBlogCommentRequest request);
    DataResponse<String> deleteComment(Long commentId);
    DatatableResponse<BlogPostResponse> getTrendingBlogPosts(int page, int limit, int days);
    DatatableResponse<BlogPostResponse> getBlogPostsByCategory(String categorySlug, int page, int limit, String sortField, String sortOrder);
    DatatableResponse<BlogPostResponse> getBlogPostsByTag(String tagSlug, int page, int limit, String sortField, String sortOrder);
    DatatableResponse<BlogPostResponse> getMyBlogPosts(int page, int limit, String sortField, String sortOrder, String status);
    DatatableResponse<BlogPostResponse> searchBlogPosts(String query, int page, int limit, String sortField, String sortOrder, String category, String tag);
    DataResponse<BlogStatsResponse> getBlogStats();
    DataResponse<BlogPostDetailResponse> getBlogPostByIdForAdmin(Long id);
    DataResponse<Map<String, String>> uploadInlineImage(MultipartFile image, Long postId);
}