package com.naskah.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank
    private String content;

    private Long parentCommentId; // For nested replies
}