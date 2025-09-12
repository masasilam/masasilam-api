package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectReactionRequest {
    @NotBlank(message = "Reaction type is required")
    private String reactionType;
}