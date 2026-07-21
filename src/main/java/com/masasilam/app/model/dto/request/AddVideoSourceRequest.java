package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddVideoSourceRequest {
    @NotBlank(message = "URL video tidak boleh kosong")
    @Size(max = 1000, message = "URL maksimal 1000 karakter")
    private String url;
    private boolean isTrailer = false;
    private Integer priority = 0;
}