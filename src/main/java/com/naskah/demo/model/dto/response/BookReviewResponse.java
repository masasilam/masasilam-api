package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long bookId;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean currentUserFeedback;
    private Boolean isOwner;
    private List<BookReviewReplyResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}