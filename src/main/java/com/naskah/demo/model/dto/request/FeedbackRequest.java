package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "Feedback type is required")
    @Pattern(regexp = "HELPFUL|NOT_HELPFUL", message = "Feedback type must be HELPFUL or NOT_HELPFUL")
    private String type;
}
