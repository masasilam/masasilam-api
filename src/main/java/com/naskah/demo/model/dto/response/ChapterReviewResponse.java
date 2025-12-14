package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userProfilePicture;
    private Integer chapterNumber;
    private String comment;
    private Boolean isSpoiler;
    private Integer likeCount;
    private Boolean isLikedByMe;
    private Long parentId;
    private List<ChapterReviewResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
