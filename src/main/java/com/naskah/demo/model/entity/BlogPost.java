package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.BlogPostStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogPost {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String featuredImage;
    private BlogPostStatus status;
    private Long authorId;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
