package com.masasilam.app.model.dto.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
public class BlogPostResponse {
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
    private String authorName;
    private String authorAvatar;
    private String categories;
    private String tags;
    private Boolean isLiked = false;
    private Integer readingTime;
}