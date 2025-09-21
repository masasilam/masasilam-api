package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReactionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long bookId;
    private String reactionType;
    private Integer rating;
    private String comment;
    private String title;
    private Long parentId;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}