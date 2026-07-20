package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityCommentResponse {
    private Long id;
    private Long activityId;
    private Long userId;
    private String username;
    private String userPhoto;
    private Long parentId;
    private String content;
    private Boolean isOwner;
    private List<ActivityCommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}