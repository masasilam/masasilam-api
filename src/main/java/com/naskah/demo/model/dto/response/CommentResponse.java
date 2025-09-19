package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponse {
    private Long id;
    private Long noteId;
    private String content;
    private String authorName;
    private Long parentCommentId;
    private Integer likeCount;
    private Boolean isLikedByUser;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;
}