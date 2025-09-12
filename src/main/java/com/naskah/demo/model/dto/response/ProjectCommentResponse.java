package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectCommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String userAvatar;
    private Long parentCommentId;
    private Integer replyCount;
    private List<ProjectCommentResponse> replies;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
