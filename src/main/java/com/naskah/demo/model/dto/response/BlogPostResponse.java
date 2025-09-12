package com.naskah.demo.model.dto.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
public class BlogPostResponse {
    private Long id;
    private String title;
    private String slug;
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
    private String authorName;
    private String authorAvatar;

    // Categories and Tags as comma-separated strings (from GROUP_CONCAT)
    private String categories;
    private String tags;

    // Engagement data
    private Boolean isLiked = false; // Will be set based on current user

    // Reading time estimation (in minutes)
    private Integer readingTime;
}
