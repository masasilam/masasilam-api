package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiscussionRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private Integer page;
    private String position;
    private Long parentId; // For replies
}