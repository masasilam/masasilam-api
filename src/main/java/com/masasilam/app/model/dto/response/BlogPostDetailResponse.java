package com.masasilam.app.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.masasilam.app.model.entity.BlogCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogPostDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String source;
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
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorBio;
    private String categories;
    private String tags;
    private String linkedBooks;
    private List<BlogCategory> categoryList;
    private List<BookResponse> bookList;
    private Boolean isLiked = false;
    private List<BlogPostResponse> relatedPosts;
    private Integer readingTime;
    private String metaDescription;
    private String metaKeywords;
}