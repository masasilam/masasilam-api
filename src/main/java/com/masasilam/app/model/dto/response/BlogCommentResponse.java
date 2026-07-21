package com.masasilam.app.model.dto.response;

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
    private Long likeCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private String userName;
    private String userAvatar;
    private Long replyCount;
    private List<BlogCommentResponse> replies;
}