package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PageCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;

    @NotBlank(message = "Comment type is required")
    private String commentType;

    private Long parentCommentId;
}