package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.BlogPostMapper;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.model.enums.*;
import com.naskah.demo.service.BlogPostService;
import com.naskah.demo.util.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostMapper blogPostMapper;

    private static final String SUCCESS = "Success";

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPosts(int page, int limit, String sortField, String sortOrder,
                                                            String status, String category, String tag, String search, Long authorId) {
        Map<String, String> allowedSortFields = new HashMap<>();
        allowedSortFields.put("createdAt", "CREATED_AT");
        allowedSortFields.put("updatedAt", "UPDATED_AT");
        allowedSortFields.put("title", "TITLE");
        allowedSortFields.put("viewCount", "VIEW_COUNT");
        allowedSortFields.put("likeCount", "LIKE_COUNT");
        allowedSortFields.put("commentCount", "COMMENT_COUNT");

        String sortColumn = allowedSortFields.getOrDefault(sortField, "CREATED_AT");
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> pageResult = blogPostMapper.getBlogPostsWithFilters(
                status, category, tag, search, authorId, offset, limit, sortColumn, sortType);

        Long currentUserId = getCurrentUserId();
        for (BlogPostResponse blogPost : pageResult) {
            enhanceBlogPostWithEngagementData(blogPost, currentUserId);
        }

        PageDataResponse<BlogPostResponse> data = new PageDataResponse<>(page, limit, pageResult.size(), pageResult);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DataResponse<BlogPostDetailResponse> getBlogPostBySlug(String slug) {
        BlogPostDetailResponse blogPost = blogPostMapper.getBlogPostBySlug(slug);
        if (blogPost == null) {
            throw new DataNotFoundException();
        }

        Long currentUserId = getCurrentUserId();

        // Increment view count
        blogPostMapper.incrementViewCount(blogPost.getId());

        // Enhance with engagement data
        enhanceBlogPostDetailWithEngagementData(blogPost, currentUserId);

        // Get related posts
        List<BlogPostResponse> relatedPosts = blogPostMapper.getRelatedBlogPosts(blogPost.getId(), 5);
        for (BlogPostResponse relatedPost : relatedPosts) {
            enhanceBlogPostWithEngagementData(relatedPost, currentUserId);
        }
        blogPost.setRelatedPosts(relatedPosts);

        return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), blogPost);
    }

    @Override
    @Transactional
    public DataResponse<BlogPostResponse> createBlogPost(CreateBlogPostRequest request, List<MultipartFile> images) {
        try {
            Long userId = getCurrentUserId();

            // Create blog post entity
            BlogPost blogPost = new BlogPost();
            blogPost.setTitle(request.getTitle());
            blogPost.setSlug(generateUniqueSlug(request.getTitle()));
            blogPost.setContent(request.getContent());
            blogPost.setExcerpt(request.getExcerpt() != null ? request.getExcerpt() :
                    generateExcerpt(request.getContent()));
            blogPost.setStatus(BlogPostStatus.valueOf(request.getStatus()));
            blogPost.setAuthorId(userId);
            blogPost.setCreatedAt(LocalDateTime.now());
            blogPost.setUpdatedAt(LocalDateTime.now());
            blogPost.setViewCount(0L);
            blogPost.setLikeCount(0L);
            blogPost.setCommentCount(0L);

            if (request.getScheduledAt() != null) {
                blogPost.setScheduledAt(request.getScheduledAt());
            }

            if (request.getStatus().equals("PUBLISHED")) {
                blogPost.setPublishedAt(LocalDateTime.now());
            }

            // Insert blog post FIRST to get the ID
            blogPostMapper.insertBlogPost(blogPost);
            log.info("Created blog post with ID: {}", blogPost.getId());

            // Process featured image AFTER insertion (now we have the ID)
            if (images != null && !images.isEmpty()) {
                try {
                    MultipartFile featuredImage = images.get(0);
                    long maxFileSize = FileUtil.parseFileSize("10MB");
                    FileUtil.validateFile(featuredImage, maxFileSize);

                    Path imagePath = FileUtil.saveFile(featuredImage, uploadDirectory, blogPost.getId());

                    // Update the blog post with the image path
                    BlogPost updateForImage = new BlogPost();
                    updateForImage.setId(blogPost.getId());
                    updateForImage.setFeaturedImage(imagePath.toString());
                    updateForImage.setUpdatedAt(LocalDateTime.now());
                    blogPostMapper.updateBlogPost(updateForImage);

                    log.info("Saved featured image: {}", imagePath.toString());
                } catch (Exception e) {
                    log.error("Failed to process featured image: {}", e.getMessage());
                    throw new RuntimeException("Failed to process featured image: " + e.getMessage());
                }
            }

            // Process categories
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                for (Long categoryId : request.getCategoryIds()) {
                    blogPostMapper.insertBlogPostCategory(blogPost.getId(), categoryId);
                }
            }

            // Process tags
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                for (String tagName : request.getTags()) {
                    // Get or create tag
                    Long tagId = blogPostMapper.getOrCreateBlogTag(tagName.trim());
                    blogPostMapper.insertBlogPostTag(blogPost.getId(), tagId);
                }
            }

            // Process linked books
            if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
                for (Long bookId : request.getBookIds()) {
                    blogPostMapper.insertBlogPostBook(blogPost.getId(), bookId);
                }
            }

            // Get the created blog post with all data
            BlogPostResponse response = blogPostMapper.getBlogPostById(blogPost.getId());
            if (response == null) {
                throw new RuntimeException("Failed to retrieve created blog post");
            }

            enhanceBlogPostWithEngagementData(response, userId);

            log.info("Successfully created blog post: {}", blogPost.getId());
            return new DataResponse<>(SUCCESS, "Blog post created successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Failed to create blog post: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<BlogPostResponse> updateBlogPost(Long id, UpdateBlogPostRequest request, List<MultipartFile> images) {
        Long userId = getCurrentUserId();

        BlogPost existingPost = blogPostMapper.getBlogPostEntityById(id);
        if (existingPost == null) {
            throw new DataNotFoundException();
        }

        if (!existingPost.getAuthorId().equals(userId) && !isAdmin()) {
            throw new UnauthorizedException();
        }

        try {
            // Update blog post
            BlogPost updatePost = new BlogPost();
            updatePost.setId(id);
            updatePost.setTitle(request.getTitle());
            updatePost.setSlug(request.getTitle().equals(existingPost.getTitle()) ?
                    existingPost.getSlug() : generateUniqueSlug(request.getTitle()));
            updatePost.setContent(request.getContent());
            updatePost.setExcerpt(request.getExcerpt() != null ? request.getExcerpt() :
                    generateExcerpt(request.getContent()));
            updatePost.setStatus(BlogPostStatus.valueOf(request.getStatus()));
            updatePost.setUpdatedAt(LocalDateTime.now());

            if (request.getScheduledAt() != null) {
                updatePost.setScheduledAt(request.getScheduledAt());
            }

            // Set published date if changing from draft to published
            if (request.getStatus().equals("PUBLISHED") &&
                    !existingPost.getStatus().equals(BlogPostStatus.PUBLISHED)) {
                updatePost.setPublishedAt(LocalDateTime.now());
            }

            // Process new featured image if provided
            if (images != null && !images.isEmpty()) {
                try {
                    MultipartFile featuredImage = images.get(0);
                    long maxFileSize = FileUtil.parseFileSize("10MB");
                    FileUtil.validateFile(featuredImage, maxFileSize);

                    // Delete old image if exists
                    if (existingPost.getFeaturedImage() != null) {
                        FileUtil.deleteFile(existingPost.getFeaturedImage());
                    }

                    Path imagePath = FileUtil.saveFile(featuredImage, uploadDirectory, id);
                    updatePost.setFeaturedImage(imagePath.toString());
                } catch (Exception e) {
                    log.error("Failed to process featured image: {}", e.getMessage());
                    throw new RuntimeException("Failed to process featured image: " + e.getMessage());
                }
            }

            blogPostMapper.updateBlogPost(updatePost);

            // Update categories
            if (request.getCategoryIds() != null) {
                blogPostMapper.deleteBlogPostCategories(id);
                for (Long categoryId : request.getCategoryIds()) {
                    blogPostMapper.insertBlogPostCategory(id, categoryId);
                }
            }

            // Update tags
            if (request.getTags() != null) {
                blogPostMapper.deleteBlogPostTags(id);
                for (String tagName : request.getTags()) {
                    Long tagId = blogPostMapper.getOrCreateBlogTag(tagName.trim());
                    blogPostMapper.insertBlogPostTag(id, tagId);
                }
            }

            // Update linked books
            if (request.getBookIds() != null) {
                blogPostMapper.deleteBlogPostBooks(id);
                for (Long bookId : request.getBookIds()) {
                    blogPostMapper.insertBlogPostBook(id, bookId);
                }
            }

            BlogPostResponse response = blogPostMapper.getBlogPostById(id);
            enhanceBlogPostWithEngagementData(response, userId);

            return new DataResponse<>(SUCCESS, "Blog post updated successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Failed to update blog post: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<String> deleteBlogPost(Long id) {
        Long userId = getCurrentUserId();

        BlogPost existingPost = blogPostMapper.getBlogPostEntityById(id);
        if (existingPost == null) {
            throw new DataNotFoundException();
        }

        if (!existingPost.getAuthorId().equals(userId) && !isAdmin()) {
            throw new UnauthorizedException();
        }

        try {
            // Delete featured image if exists
            if (existingPost.getFeaturedImage() != null) {
                FileUtil.deleteFile(existingPost.getFeaturedImage());
            }

            // Delete associations
            blogPostMapper.deleteBlogPostCategories(id);
            blogPostMapper.deleteBlogPostTags(id);
            blogPostMapper.deleteBlogPostBooks(id);
            blogPostMapper.deleteBlogPostLikes(id);
            blogPostMapper.deleteBlogPostComments(id);

            // Delete the blog post
            blogPostMapper.deleteBlogPost(id);

            return new DataResponse<>(SUCCESS, "Blog post deleted successfully",
                    HttpStatus.OK.value(), "Blog post deleted");

        } catch (Exception e) {
            log.error("Failed to delete blog post: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete blog post: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DataResponse<BlogPostLikeResponse> toggleLike(Long blogPostId) {
        Long userId = getCurrentUserId();

        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) {
            throw new DataNotFoundException();
        }

        boolean isLiked = blogPostMapper.isBlogPostLikedByUser(blogPostId, userId);

        if (isLiked) {
            blogPostMapper.deleteBlogPostLike(blogPostId, userId);
            blogPostMapper.decrementLikeCount(blogPostId);
        } else {
            blogPostMapper.insertBlogPostLike(blogPostId, userId);
            blogPostMapper.incrementLikeCount(blogPostId);
        }

        Long totalLikes = blogPostMapper.getBlogPostLikeCount(blogPostId);

        BlogPostLikeResponse response = new BlogPostLikeResponse();
        response.setBlogPostId(blogPostId);
        response.setIsLiked(!isLiked);
        response.setTotalLikes(totalLikes);

        return new DataResponse<>(SUCCESS, !isLiked ? "Post liked" : "Post unliked",
                HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<BlogCommentResponse> addComment(Long blogPostId, CreateBlogCommentRequest request) {
        Long userId = getCurrentUserId();

        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) {
            throw new DataNotFoundException();
        }

        BlogComment comment = new BlogComment();
        comment.setBlogPostId(blogPostId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        blogPostMapper.insertBlogComment(comment);
        blogPostMapper.incrementCommentCount(blogPostId);

        BlogCommentResponse response = blogPostMapper.getBlogCommentById(comment.getId());

        return new DataResponse<>(SUCCESS, "Comment added successfully",
                HttpStatus.CREATED.value(), response);
    }

    @Override
    public DatatableResponse<BlogCommentResponse> getBlogPostComments(Long blogPostId, int page, int limit,
                                                                      String sortField, String sortOrder) {
        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) {
            throw new DataNotFoundException();
        }

        String sortColumn = sortField.equals("createdAt") ? "CREATED_AT" : "CREATED_AT";
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogCommentResponse> comments = blogPostMapper.getBlogPostComments(
                blogPostId, offset, limit, sortColumn, sortType);

        PageDataResponse<BlogCommentResponse> data = new PageDataResponse<>(page, limit, comments.size(), comments);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    @Transactional
    public DataResponse<BlogCommentResponse> updateComment(Long commentId, UpdateBlogCommentRequest request) {
        Long userId = getCurrentUserId();

        BlogComment existingComment = blogPostMapper.getBlogCommentEntityById(commentId);
        if (existingComment == null) {
            throw new DataNotFoundException();
        }

        if (!existingComment.getUserId().equals(userId) && !isAdmin()) {
            throw new UnauthorizedException();
        }

        BlogComment updateComment = new BlogComment();
        updateComment.setId(commentId);
        updateComment.setContent(request.getContent());
        updateComment.setUpdatedAt(LocalDateTime.now());

        blogPostMapper.updateBlogComment(updateComment);

        BlogCommentResponse response = blogPostMapper.getBlogCommentById(commentId);

        return new DataResponse<>(SUCCESS, "Comment updated successfully",
                HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<String> deleteComment(Long commentId) {
        Long userId = getCurrentUserId();

        BlogComment existingComment = blogPostMapper.getBlogCommentEntityById(commentId);
        if (existingComment == null) {
            throw new DataNotFoundException();
        }

        if (!existingComment.getUserId().equals(userId) && !isAdmin()) {
            throw new UnauthorizedException();
        }

        blogPostMapper.deleteBlogComment(commentId);
        blogPostMapper.decrementCommentCount(existingComment.getBlogPostId());

        return new DataResponse<>(SUCCESS, "Comment deleted successfully",
                HttpStatus.OK.value(), "Comment deleted");
    }

    @Override
    public DatatableResponse<BlogPostResponse> getTrendingBlogPosts(int page, int limit, int days) {
        int offset = (page - 1) * limit;

        List<BlogPostResponse> trendingPosts = blogPostMapper.getTrendingBlogPosts(days, offset, limit);

        Long currentUserId = getCurrentUserId();
        for (BlogPostResponse blogPost : trendingPosts) {
            enhanceBlogPostWithEngagementData(blogPost, currentUserId);
        }

        PageDataResponse<BlogPostResponse> data = new PageDataResponse<>(page, limit, trendingPosts.size(), trendingPosts);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPostsByCategory(String categorySlug, int page, int limit,
                                                                      String sortField, String sortOrder) {
        String sortColumn = sortField.equals("createdAt") ? "CREATED_AT" : "CREATED_AT";
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsByCategory(
                categorySlug, offset, limit, sortColumn, sortType);

        Long currentUserId = getCurrentUserId();
        for (BlogPostResponse blogPost : posts) {
            enhanceBlogPostWithEngagementData(blogPost, currentUserId);
        }

        PageDataResponse<BlogPostResponse> data = new PageDataResponse<>(page, limit, posts.size(), posts);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPostsByTag(String tagSlug, int page, int limit, String sortField, String sortOrder) {
        return null;
    }

    @Override
    public DatatableResponse<BlogPostResponse> getMyBlogPosts(int page, int limit, String sortField,
                                                              String sortOrder, String status) {
        Long userId = getCurrentUserId();
        String sortColumn = sortField.equals("createdAt") ? "CREATED_AT" : "CREATED_AT";
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsByAuthor(
                userId, status, offset, limit, sortColumn, sortType);

        for (BlogPostResponse blogPost : posts) {
            enhanceBlogPostWithEngagementData(blogPost, userId);
        }

        PageDataResponse<BlogPostResponse> data = new PageDataResponse<>(page, limit, posts.size(), posts);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DatatableResponse<BlogPostResponse> searchBlogPosts(String query, int page, int limit,
                                                               String sortField, String sortOrder,
                                                               String category, String tag) {
        String sortColumn = sortField.equals("createdAt") ? "CREATED_AT" : "CREATED_AT";
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> posts = blogPostMapper.searchBlogPosts(
                query, category, tag, offset, limit, sortColumn, sortType);

        Long currentUserId = getCurrentUserId();
        for (BlogPostResponse blogPost : posts) {
            enhanceBlogPostWithEngagementData(blogPost, currentUserId);
        }

        PageDataResponse<BlogPostResponse> data = new PageDataResponse<>(page, limit, posts.size(), posts);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    @Override
    public DataResponse<BlogStatsResponse> getBlogStats() {
        Long userId = getCurrentUserId();

        BlogStatsResponse stats = new BlogStatsResponse();
        stats.setTotalPosts(blogPostMapper.getTotalBlogPostsByUser(userId));
        stats.setTotalViews(blogPostMapper.getTotalViewsByUser(userId));
        stats.setTotalLikes(blogPostMapper.getTotalLikesByUser(userId));
        stats.setTotalComments(blogPostMapper.getTotalCommentsByUser(userId));
        stats.setPublishedPosts(blogPostMapper.getPublishedBlogPostsByUser(userId));
        stats.setDraftPosts(blogPostMapper.getDraftBlogPostsByUser(userId));

        // Get monthly stats for the last 12 months
        List<MonthlyStatsResponse> monthlyStats = blogPostMapper.getMonthlyStatsByUser(userId, 12);
        stats.setMonthlyStats(monthlyStats);

        return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), stats);
    }

    // Helper methods
    private void enhanceBlogPostWithEngagementData(BlogPostResponse blogPost, Long currentUserId) {
        if (currentUserId != null) {
            boolean isLiked = blogPostMapper.isBlogPostLikedByUser(blogPost.getId(), currentUserId);
            blogPost.setIsLiked(isLiked);
        }
    }

    private void enhanceBlogPostDetailWithEngagementData(BlogPostDetailResponse blogPost, Long currentUserId) {
        if (currentUserId != null) {
            boolean isLiked = blogPostMapper.isBlogPostLikedByUser(blogPost.getId(), currentUserId);
            blogPost.setIsLiked(isLiked);
        }
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = baseSlug;
        int counter = 1;

        while (blogPostMapper.isSlugExists(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    private String generateExcerpt(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Remove HTML tags and get first 150 characters
        String plainText = content.replaceAll("<[^>]*>", "").trim();
        if (plainText.length() <= 150) {
            return plainText;
        }

        return plainText.substring(0, 150) + "...";
    }

    private Long getCurrentUserId() {
        // Implementation depends on your security context
        // This is a placeholder - replace with actual implementation
        return 1L; // Placeholder
    }

    private boolean isAdmin() {
        // Implementation depends on your security context
        // This is a placeholder - replace with actual implementation
        return false; // Placeholder
    }
}