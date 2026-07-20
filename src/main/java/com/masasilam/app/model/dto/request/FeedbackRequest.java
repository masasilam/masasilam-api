package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotNull(message = "Feedback is required")
    private Boolean isHelpful;
}
