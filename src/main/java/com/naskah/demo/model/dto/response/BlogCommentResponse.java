package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogCommentResponse {
    private Long id;
    private Long blogPostId;
    private Long userId;
    private String content;
    private Long parentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // User information
    private String userName;
    private String userAvatar;

    // Reply count for nested comments
    private Long replyCount;

    // Nested replies (if needed)
    private List<BlogCommentResponse> replies;
}
