package com.naskah.demo.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReviewRequest {
    private String title;

    @NotBlank(message = "Review content is required")
    @Size(min = 10, max = 5000, message = "Review must be between 10 and 5000 characters")
    private String content;
}
