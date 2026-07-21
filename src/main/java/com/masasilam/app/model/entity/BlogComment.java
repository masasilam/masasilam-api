package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogComment {
    private Long id;
    private Long blogPostId;
    private Long userId;
    private String content;
    private Long parentId;
    private Boolean isApproved;
    private Long likeCount;
    private LocalDateTime createdAt;
}