package com.naskah.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PageReactionRequest {
    @NotBlank(message = "Reaction type is required")
    private String reactionType;
}