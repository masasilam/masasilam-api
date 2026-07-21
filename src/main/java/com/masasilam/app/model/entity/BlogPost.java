package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.BlogPostStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogPost {
    private Long id;
    private Long authorId;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String source;
    private String featuredImage;
    private BlogPostStatus status;
    private Boolean isFeatured;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Integer readingTimeMinutes;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}