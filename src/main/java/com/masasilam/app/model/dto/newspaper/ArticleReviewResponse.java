package com.masasilam.app.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long articleId;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean isOwner;
    private Boolean currentUserFeedback;
    private List<ArticleReviewReplyResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}