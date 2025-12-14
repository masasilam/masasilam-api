package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookReviewRequest {
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Review content is required")
    @Size(min = 1, max = 5000, message = "Review must be between 10 and 5000 characters")
    private String content;
}
