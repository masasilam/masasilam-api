package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotNull(message = "Feedback is required")
    private Boolean isHelpful;
}
