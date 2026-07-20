package com.naskah.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDiscussionRequest {
    @Size(max = 500)
    private String title;

    @NotBlank
    private String content;

    private Long scheduleId;
    private Long parentId;
    private String entityType;
    private Long entityId;
    private String cfiReference;
}