package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PageCommentResponse {
    private Long id;
    private String content;
    private String commentType;
    private Long userId;
    private String username;
    private String userAvatar;
    private Long parentCommentId;
    private Integer replyCount;
    private List<PageCommentResponse> replies;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}