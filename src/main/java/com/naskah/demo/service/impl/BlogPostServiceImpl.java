package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.ForbiddenException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.BlogPostMapper;
import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.model.enums.*;
import com.naskah.demo.service.BlogPostService;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostMapper blogPostMapper;
    private final UserMapper userMapper;
    private final FileUtil fileUtil;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    // ─── Auth helpers ────────────────────────────────────────────────────────

    private void requireAuth() {
        if (headerHolder.getUsername() == null || headerHolder.getUsername().isEmpty())
            throw new UnauthorizedException();
    }

    private void requireAdmin() {
        requireAuth();
        if (headerHolder.getRoles() == null ||
                !Arrays.asList(headerHolder.getRoles()).contains("ADMIN"))
            throw new ForbiddenException();
    }

    private boolean isAdmin() {
        return headerHolder.getRoles() != null &&
                Arrays.asList(headerHolder.getRoles()).contains("ADMIN");
    }

    private User getCurrentUser() {
        requireAuth();
        User user = userMapper.findUserByUsername(headerHolder.getUsername());
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long getCurrentUserIdOrNull() {
        try { return getCurrentUser().getId(); } catch (Exception e) { return null; }
    }

    // ─── Public listing ───────────────────────────────────────────────────────

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPosts(int page, int limit, String sortField,
                                                            String sortOrder, String status,
                                                            String category, String tag,
                                                            String search, Long authorId) {
        return fetchList(page, limit, sortField, sortOrder, status, category, tag, search, authorId, false);
    }

    // ─── Admin: my-posts ─────────────────────────────────────────────────────

    @Override
    public DatatableResponse<BlogPostResponse> getMyBlogPosts(int page, int limit, String sortField,
                                                              String sortOrder, String status) {
        requireAdmin();
        Long userId = getCurrentUser().getId();
        String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsWithFilters(
                status, null, null, null, userId, true, offset, limit, "CREATED_AT", sortType);
        posts.forEach(p -> enhanceLikeStatus(p, userId));

        return datatableOf(page, limit, posts);
    }

    @Override
    public DataResponse<BlogPostDetailResponse> getBlogPostByIdForAdmin(Long id) {
        requireAdmin();
        BlogPostDetailResponse post = blogPostMapper.getBlogPostByIdForAdmin(id);
        if (post == null) throw new DataNotFoundException();
        return dataOf(post);
    }

    // ─── ✅ Upload inline image ke Cloudinary ─────────────────────────────────

    /**
     * Upload gambar inline blog ke Cloudinary.
     * Return URL permanen (https://res.cloudinary.com/...) — bukan path lokal.
     *
     * @param image  File gambar
     * @param postId ID artikel (opsional, null jika artikel baru belum tersimpan)
     */
    @Override
    public DataResponse<Map<String, String>> uploadInlineImage(MultipartFile image, Long postId) {
        requireAuth();
        try {
            fileUtil.validateFile(image, fileUtil.parseFileSize("10MB"));
            // ✅ Cloudinary — sama seperti cover buku dan foto author
            String cloudinaryUrl = fileUtil.uploadBlogImage(image, postId);
            log.info("Blog image uploaded to Cloudinary: {}", cloudinaryUrl);
            return dataOf(Map.of("url", cloudinaryUrl));
        } catch (Exception e) {
            log.error("Error uploading blog image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    // ─── Get by slug ─────────────────────────────────────────────────────────

    @Override
    public DataResponse<BlogPostDetailResponse> getBlogPostBySlug(String slug) {
        BlogPostDetailResponse post = blogPostMapper.getBlogPostBySlug(slug);
        if (post == null) throw new DataNotFoundException();

        blogPostMapper.incrementViewCount(post.getId());

        Long uid = getCurrentUserIdOrNull();
        if (uid != null) post.setIsLiked(blogPostMapper.isBlogPostLikedByUser(post.getId(), uid));

        List<BlogPostResponse> related = blogPostMapper.getRelatedBlogPosts(post.getId(), 5);
        related.forEach(p -> enhanceLikeStatus(p, uid));
        post.setRelatedPosts(related);

        return dataOf(post);
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<BlogPostResponse> createBlogPost(CreateBlogPostRequest request,
                                                         List<MultipartFile> images) {
        requireAdmin();
        Long userId = getCurrentUser().getId();

        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle());
        post.setSlug(generateUniqueSlug(request.getTitle()));
        post.setContent(request.getContent());
        post.setExcerpt(request.getExcerpt() != null ? request.getExcerpt() : generateExcerpt(request.getContent()));
        post.setStatus(BlogPostStatus.valueOf(request.getStatus()));
        post.setAuthorId(userId);
        post.setIsFeatured(false);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setCommentCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        if (request.getScheduledAt() != null) post.setScheduledAt(request.getScheduledAt());
        if ("PUBLISHED".equals(request.getStatus())) post.setPublishedAt(LocalDateTime.now());

        blogPostMapper.insertBlogPost(post);

        if (images != null && !images.isEmpty())
            saveFeaturedImage(images.getFirst(), post.getId(), null);

        saveRelations(post.getId(), request.getCategoryIds(), request.getTags(), request.getBookIds());

        BlogPostResponse response = blogPostMapper.getBlogPostById(post.getId());
        enhanceLikeStatus(response, userId);
        return new DataResponse<>(SUCCESS, "Blog post created successfully", HttpStatus.CREATED.value(), response);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<BlogPostResponse> updateBlogPost(Long id, UpdateBlogPostRequest request,
                                                         List<MultipartFile> images) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        BlogPost existing = blogPostMapper.getBlogPostEntityById(id);
        if (existing == null) throw new DataNotFoundException();
        if (!existing.getAuthorId().equals(userId) && !isAdmin()) throw new ForbiddenException();

        BlogPost update = new BlogPost();
        update.setId(id);
        update.setTitle(request.getTitle());
        update.setSlug(request.getTitle().equals(existing.getTitle())
                ? existing.getSlug() : generateUniqueSlug(request.getTitle()));
        update.setContent(request.getContent());
        update.setExcerpt(request.getExcerpt() != null ? request.getExcerpt() : generateExcerpt(request.getContent()));
        update.setStatus(BlogPostStatus.valueOf(request.getStatus()));
        update.setUpdatedAt(LocalDateTime.now());
        if (request.getScheduledAt() != null) update.setScheduledAt(request.getScheduledAt());
        if ("PUBLISHED".equals(request.getStatus()) && !existing.getStatus().equals(BlogPostStatus.PUBLISHED))
            update.setPublishedAt(LocalDateTime.now());

        if (images != null && !images.isEmpty())
            saveFeaturedImage(images.getFirst(), id, existing.getFeaturedImage());

        blogPostMapper.updateBlogPost(update);
        updateRelations(id, request.getCategoryIds(), request.getTags(), request.getBookIds());

        BlogPostResponse response = blogPostMapper.getBlogPostById(id);
        enhanceLikeStatus(response, userId);
        return new DataResponse<>(SUCCESS, "Blog post updated successfully", HttpStatus.OK.value(), response);
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<String> deleteBlogPost(Long id) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        BlogPost existing = blogPostMapper.getBlogPostEntityById(id);
        if (existing == null) throw new DataNotFoundException();
        if (!existing.getAuthorId().equals(userId) && !isAdmin()) throw new ForbiddenException();

        if (existing.getFeaturedImage() != null) fileUtil.deleteFile(existing.getFeaturedImage());
        blogPostMapper.deleteBlogPostCategories(id);
        blogPostMapper.deleteBlogPostTags(id);
        blogPostMapper.deleteBlogPostBooks(id);
        blogPostMapper.deleteBlogPostLikes(id);
        blogPostMapper.deleteBlogPostComments(id);
        blogPostMapper.deleteBlogPost(id);

        return new DataResponse<>(SUCCESS, "Blog post deleted successfully", HttpStatus.OK.value(), "Blog post deleted");
    }

    // ─── Like ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<BlogPostLikeResponse> toggleLike(Long blogPostId) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) throw new DataNotFoundException();

        boolean liked = blogPostMapper.isBlogPostLikedByUser(blogPostId, userId);
        if (liked) {
            blogPostMapper.deleteBlogPostLike(blogPostId, userId);
            blogPostMapper.decrementLikeCount(blogPostId);
        } else {
            blogPostMapper.insertBlogPostLike(blogPostId, userId);
            blogPostMapper.incrementLikeCount(blogPostId);
        }

        BlogPostLikeResponse res = new BlogPostLikeResponse();
        res.setBlogPostId(blogPostId);
        res.setIsLiked(!liked);
        res.setTotalLikes(blogPostMapper.getBlogPostLikeCount(blogPostId));
        return new DataResponse<>(SUCCESS, !liked ? "Post liked" : "Post unliked", HttpStatus.OK.value(), res);
    }

    // ─── Comments ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DataResponse<BlogCommentResponse> addComment(Long blogPostId, CreateBlogCommentRequest request) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) throw new DataNotFoundException();

        BlogComment comment = new BlogComment();
        comment.setBlogPostId(blogPostId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setCreatedAt(LocalDateTime.now());

        blogPostMapper.insertBlogComment(comment);
        blogPostMapper.incrementCommentCount(blogPostId);
        return new DataResponse<>(SUCCESS, "Comment added successfully",
                HttpStatus.CREATED.value(), blogPostMapper.getBlogCommentById(comment.getId()));
    }

    @Override
    public DatatableResponse<BlogCommentResponse> getBlogPostComments(Long blogPostId, int page, int limit,
                                                                      String sortField, String sortOrder) {
        if (blogPostMapper.getBlogPostEntityById(blogPostId) == null) throw new DataNotFoundException();
        int offset = (page - 1) * limit;
        String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        List<BlogCommentResponse> comments = blogPostMapper.getBlogPostComments(blogPostId, offset, limit, "CREATED_AT", sortType);
        return datatableOf(page, limit, comments);
    }

    @Override
    @Transactional
    public DataResponse<BlogCommentResponse> updateComment(Long commentId, UpdateBlogCommentRequest request) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        BlogComment existing = blogPostMapper.getBlogCommentEntityById(commentId);
        if (existing == null) throw new DataNotFoundException();
        if (!existing.getUserId().equals(userId) && !isAdmin()) throw new ForbiddenException();

        BlogComment update = new BlogComment();
        update.setId(commentId);
        update.setContent(request.getContent());
        blogPostMapper.updateBlogComment(update);
        return dataOf(blogPostMapper.getBlogCommentById(commentId));
    }

    @Override
    @Transactional
    public DataResponse<String> deleteComment(Long commentId) {
        requireAuth();
        Long userId = getCurrentUser().getId();
        BlogComment existing = blogPostMapper.getBlogCommentEntityById(commentId);
        if (existing == null) throw new DataNotFoundException();
        if (!existing.getUserId().equals(userId) && !isAdmin()) throw new ForbiddenException();

        blogPostMapper.deleteBlogComment(commentId);
        blogPostMapper.decrementCommentCount(existing.getBlogPostId());
        return new DataResponse<>(SUCCESS, "Comment deleted successfully", HttpStatus.OK.value(), "Comment deleted");
    }

    // ─── Trending / Category / Tag / Search ──────────────────────────────────

    @Override
    public DatatableResponse<BlogPostResponse> getTrendingBlogPosts(int page, int limit, int days) {
        int offset = (page - 1) * limit;
        List<BlogPostResponse> posts = blogPostMapper.getTrendingBlogPosts(days, offset, limit);
        Long uid = getCurrentUserIdOrNull();
        posts.forEach(p -> enhanceLikeStatus(p, uid));
        return datatableOf(page, limit, posts);
    }

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPostsByCategory(String categorySlug, int page, int limit,
                                                                      String sortField, String sortOrder) {
        int offset = (page - 1) * limit;
        String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsByCategory(categorySlug, offset, limit, "CREATED_AT", sortType);
        Long uid = getCurrentUserIdOrNull();
        posts.forEach(p -> enhanceLikeStatus(p, uid));
        return datatableOf(page, limit, posts);
    }

    @Override
    public DatatableResponse<BlogPostResponse> getBlogPostsByTag(String tagSlug, int page, int limit,
                                                                 String sortField, String sortOrder) {
        int offset = (page - 1) * limit;
        String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsByTag(tagSlug, offset, limit, "CREATED_AT", sortType);
        Long uid = getCurrentUserIdOrNull();
        posts.forEach(p -> enhanceLikeStatus(p, uid));
        return datatableOf(page, limit, posts);
    }

    @Override
    public DatatableResponse<BlogPostResponse> searchBlogPosts(String query, int page, int limit,
                                                               String sortField, String sortOrder,
                                                               String category, String tag) {
        int offset = (page - 1) * limit;
        String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        List<BlogPostResponse> posts = blogPostMapper.searchBlogPosts(query, category, tag, offset, limit, "CREATED_AT", sortType);
        Long uid = getCurrentUserIdOrNull();
        posts.forEach(p -> enhanceLikeStatus(p, uid));
        return datatableOf(page, limit, posts);
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    @Override
    public DataResponse<BlogStatsResponse> getBlogStats() {
        requireAdmin();
        Long userId = getCurrentUser().getId();

        BlogStatsResponse stats = new BlogStatsResponse();
        stats.setTotalPosts(blogPostMapper.getTotalBlogPostsByUser(userId));
        stats.setTotalViews(blogPostMapper.getTotalViewsByUser(userId));
        stats.setTotalLikes(blogPostMapper.getTotalLikesByUser(userId));
        stats.setTotalComments(blogPostMapper.getTotalCommentsByUser(userId));
        stats.setPublishedPosts(blogPostMapper.getPublishedBlogPostsByUser(userId));
        stats.setDraftPosts(blogPostMapper.getDraftBlogPostsByUser(userId));
        stats.setMonthlyStats(blogPostMapper.getMonthlyStatsByUser(userId, 12));
        return dataOf(stats);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private DatatableResponse<BlogPostResponse> fetchList(int page, int limit, String sortField,
                                                          String sortOrder, String status,
                                                          String category, String tag,
                                                          String search, Long authorId, boolean adminMode) {
        Map<String, String> sortMap = Map.of(
                "createdAt",    "CREATED_AT",
                "updatedAt",    "UPDATED_AT",
                "title",        "TITLE",
                "viewCount",    "VIEW_COUNT",
                "likeCount",    "LIKE_COUNT",
                "commentCount", "COMMENT_COUNT"
        );
        String sortColumn = sortMap.getOrDefault(sortField, "CREATED_AT");
        String sortType   = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<BlogPostResponse> posts = blogPostMapper.getBlogPostsWithFilters(
                status, category, tag, search, authorId, adminMode, offset, limit, sortColumn, sortType);
        Long uid = getCurrentUserIdOrNull();
        posts.forEach(p -> enhanceLikeStatus(p, uid));
        return datatableOf(page, limit, posts);
    }

    /**
     * Upload featured image ke Cloudinary (bukan local disk).
     * Hapus gambar lama jika ada, lalu upload baru.
     */
    private void saveFeaturedImage(MultipartFile file, Long postId, String existingUrl) {
        try {
            fileUtil.validateFile(file, fileUtil.parseFileSize("10MB"));
            if (existingUrl != null) fileUtil.deleteFile(existingUrl);

            // ✅ Upload cover artikel ke Cloudinary via uploadBlogImage
            String cloudinaryUrl = fileUtil.uploadBlogImage(file, postId);

            BlogPost imgUpdate = new BlogPost();
            imgUpdate.setId(postId);
            imgUpdate.setFeaturedImage(cloudinaryUrl);
            imgUpdate.setUpdatedAt(LocalDateTime.now());
            blogPostMapper.updateBlogPost(imgUpdate);
        } catch (Exception e) {
            log.error("Failed to process featured image: {}", e.getMessage());
            throw new RuntimeException("Failed to process featured image: " + e.getMessage());
        }
    }

    private void saveRelations(Long postId, List<Long> categoryIds, List<String> tags, List<Long> bookIds) {
        if (categoryIds != null)
            categoryIds.forEach(cid -> blogPostMapper.insertBlogPostCategory(postId, cid));
        if (tags != null)
            tags.forEach(t -> blogPostMapper.insertBlogPostTag(postId, blogPostMapper.getOrCreateBlogTag(t.trim())));
        if (bookIds != null)
            bookIds.forEach(bid -> blogPostMapper.insertBlogPostBook(postId, bid));
    }

    private void updateRelations(Long postId, List<Long> categoryIds, List<String> tags, List<Long> bookIds) {
        if (categoryIds != null) {
            blogPostMapper.deleteBlogPostCategories(postId);
            categoryIds.forEach(cid -> blogPostMapper.insertBlogPostCategory(postId, cid));
        }
        if (tags != null) {
            blogPostMapper.deleteBlogPostTags(postId);
            tags.forEach(t -> blogPostMapper.insertBlogPostTag(postId, blogPostMapper.getOrCreateBlogTag(t.trim())));
        }
        if (bookIds != null) {
            blogPostMapper.deleteBlogPostBooks(postId);
            bookIds.forEach(bid -> blogPostMapper.insertBlogPostBook(postId, bid));
        }
    }

    private void enhanceLikeStatus(BlogPostResponse post, Long userId) {
        if (userId != null)
            post.setIsLiked(blogPostMapper.isBlogPostLikedByUser(post.getId(), userId));
    }

    private String generateUniqueSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        String slug = base;
        int counter = 1;
        while (blogPostMapper.isSlugExists(slug)) slug = base + "-" + counter++;
        return slug;
    }

    private String generateExcerpt(String content) {
        if (content == null || content.isEmpty()) return "";
        String plain = content.replaceAll("<[^>]*>", "").trim();
        return plain.length() <= 150 ? plain : plain.substring(0, 150) + "...";
    }

    private <T> DataResponse<T> dataOf(T data) {
        return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    private <T> DatatableResponse<T> datatableOf(int page, int limit, List<T> list) {
        PageDataResponse<T> data = new PageDataResponse<>(page, limit, list.size(), list);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }
}