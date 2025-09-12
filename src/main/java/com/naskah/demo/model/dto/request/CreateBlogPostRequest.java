package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBlogPostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "DRAFT|PUBLISHED|SCHEDULED|ARCHIVED", message = "Invalid status")
    private String status;

    private LocalDateTime scheduledAt;

    private List<Long> categoryIds;

    private List<String> tags;

    private List<Long> bookIds;
}
