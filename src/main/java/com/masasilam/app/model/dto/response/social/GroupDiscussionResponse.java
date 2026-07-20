package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupDiscussionResponse {
    private Long id;
    private Long groupId;
    private Long scheduleId;
    private Long userId;
    private String username;
    private String userPhoto;
    private String userRole;  // owner, moderator, member
    private Long parentId;
    private String title;
    private String content;
    private String entityType;
    private Long entityId;
    private String cfiReference;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isPinned;
    private Boolean isOwner;
    private Boolean isLikedByMe;
    private List<GroupDiscussionResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}