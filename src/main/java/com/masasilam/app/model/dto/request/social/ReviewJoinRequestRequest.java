package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewJoinRequestRequest {
    @NotBlank
    private String action;
    private String note;
}