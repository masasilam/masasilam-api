package com.naskah.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QARequest {
    @NotBlank
    private String question;

    private Integer contextPage; // Optional context
    private String contextSection;
}