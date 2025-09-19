package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank
    private String reactionType; // LIKE, LOVE, STAR, THUMB_UP, etc.

    private Integer page;
    private String position;
}