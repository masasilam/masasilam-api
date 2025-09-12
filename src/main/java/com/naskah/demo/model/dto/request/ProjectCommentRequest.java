package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;

    private Long parentCommentId;
}