package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitWorkRequest {
    @NotBlank(message = "Work content is required")
    private String workContent;
    private String notes;
    private Boolean isCompleted = true;
}