package com.masasilam.app.model.dto.monograph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMonographStatusRequest {
    @NotBlank
    @Pattern(regexp = "draft|in_review|published", message = "Status harus salah satu dari: draft, in_review, published")
    private String status;
}