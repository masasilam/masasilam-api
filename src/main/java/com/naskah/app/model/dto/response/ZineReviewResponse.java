package com.naskah.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ZineReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long zineId;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean isOwner;
    private Boolean currentUserFeedback;
    private List<ZineReviewReplyResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}