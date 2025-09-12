package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitWorkRequest {

    @NotBlank(message = "Work content is required")
    private String workContent;

    private String notes;

    private Boolean isCompleted = true;
}