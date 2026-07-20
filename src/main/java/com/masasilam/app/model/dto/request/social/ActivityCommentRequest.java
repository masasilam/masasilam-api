package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityCommentRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;

    private Long parentId;
}