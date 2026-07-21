package com.masasilam.app.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareArticleRequest {
    @NotBlank(message = "Platform is required")
    private String platform;
}
