package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnotationCommentResponse {
    private Long id;
    private Long annotationId;
    private Long userId;
    private String username;
    private String userPhoto;
    private Long parentId;
    private String content;
    private Boolean isOwner;
    private List<AnnotationCommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}