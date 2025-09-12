package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogPostMapper {

    // Blog Post CRUD Operations
    void insertBlogPost(BlogPost blogPost);
    void updateBlogPost(BlogPost blogPost);
    void deleteBlogPost(@Param("id") Long id);

    BlogPost getBlogPostEntityById(@Param("id") Long id);
    BlogPostResponse getBlogPostById(@Param("id") Long id);
    BlogPostDetailResponse getBlogPostBySlug(@Param("slug") String slug);

    boolean isSlugExists(@Param("slug") String slug);

    // Blog Post Listings with Filters
    List<BlogPostResponse> getBlogPostsWithFilters(
            @Param("status") String status,
            @Param("category") String category,
            @Param("tag") String tag,
            @Param("search") String search,
            @Param("authorId") Long authorId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    List<BlogPostResponse> getTrendingBlogPosts(
            @Param("days") int days,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    List<BlogPostResponse> getBlogPostsByCategory(
            @Param("categorySlug") String categorySlug,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    List<BlogPostResponse> getBlogPostsByTag(
            @Param("tagSlug") String tagSlug,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    List<BlogPostResponse> getBlogPostsByAuthor(
            @Param("authorId") Long authorId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    List<BlogPostResponse> searchBlogPosts(
            @Param("query") String query,
            @Param("category") String category,
            @Param("tag") String tag,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    List<BlogPostResponse> getRelatedBlogPosts(
            @Param("blogPostId") Long blogPostId,
            @Param("limit") int limit
    );

    // Blog Post Categories
    void insertBlogPostCategory(@Param("blogPostId") Long blogPostId, @Param("categoryId") Long categoryId);
    void deleteBlogPostCategories(@Param("blogPostId") Long blogPostId);

    // Blog Post Tags
    void insertBlogPostTag(@Param("blogPostId") Long blogPostId, @Param("tagId") Long tagId);
    void deleteBlogPostTags(@Param("blogPostId") Long blogPostId);
    Long getOrCreateBlogTag(@Param("tagName") String tagName);

    // Blog Post Books
    void insertBlogPostBook(@Param("blogPostId") Long blogPostId, @Param("bookId") Long bookId);
    void deleteBlogPostBooks(@Param("blogPostId") Long blogPostId);

    // Blog Post Likes
    void insertBlogPostLike(@Param("blogPostId") Long blogPostId, @Param("userId") Long userId);
    void deleteBlogPostLike(@Param("blogPostId") Long blogPostId, @Param("userId") Long userId);
    void deleteBlogPostLikes(@Param("blogPostId") Long blogPostId);
    boolean isBlogPostLikedByUser(@Param("blogPostId") Long blogPostId, @Param("userId") Long userId);
    Long getBlogPostLikeCount(@Param("blogPostId") Long blogPostId);
    void incrementLikeCount(@Param("blogPostId") Long blogPostId);
    void decrementLikeCount(@Param("blogPostId") Long blogPostId);

    // Blog Comments CRUD
    void insertBlogComment(BlogComment blogComment);
    void updateBlogComment(BlogComment blogComment);
    void deleteBlogComment(@Param("id") Long id);
    void deleteBlogPostComments(@Param("blogPostId") Long blogPostId);

    BlogComment getBlogCommentEntityById(@Param("id") Long id);
    BlogCommentResponse getBlogCommentById(@Param("id") Long id);

    List<BlogCommentResponse> getBlogPostComments(
            @Param("blogPostId") Long blogPostId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType
    );

    void incrementCommentCount(@Param("blogPostId") Long blogPostId);
    void decrementCommentCount(@Param("blogPostId") Long blogPostId);

    // View Count
    void incrementViewCount(@Param("blogPostId") Long blogPostId);

    // Statistics
    Long getTotalBlogPostsByUser(@Param("userId") Long userId);
    Long getTotalViewsByUser(@Param("userId") Long userId);
    Long getTotalLikesByUser(@Param("userId") Long userId);
    Long getTotalCommentsByUser(@Param("userId") Long userId);
    Long getPublishedBlogPostsByUser(@Param("userId") Long userId);
    Long getDraftBlogPostsByUser(@Param("userId") Long userId);

    List<MonthlyStatsResponse> getMonthlyStatsByUser(
            @Param("userId") Long userId,
            @Param("months") int months
    );
}