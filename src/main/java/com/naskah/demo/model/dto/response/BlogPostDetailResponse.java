package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naskah.demo.model.entity.BlogCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogPostDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String content; // Full content for detail view
    private String excerpt;
    private String featuredImage;
    private String status;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Author information
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorBio;

    // Categories and Tags as structured data
    private String categories; // Will be parsed from GROUP_CONCAT
    private String tags; // Will be parsed from GROUP_CONCAT
    private String linkedBooks; // Will be parsed from GROUP_CONCAT

    // Parsed structured data
    private List<BlogCategory> categoryList;
    private List<BookResponse> bookList;

    // Engagement data
    private Boolean isLiked = false;

    // Related content
    private List<BlogPostResponse> relatedPosts;

    // Reading time estimation
    private Integer readingTime;

    // SEO data
    private String metaDescription;
    private String metaKeywords;
}
