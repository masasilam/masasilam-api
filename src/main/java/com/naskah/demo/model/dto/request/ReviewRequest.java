package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {
    private String title;

    @NotBlank(message = "Review content is required")
    private String comment;
}
