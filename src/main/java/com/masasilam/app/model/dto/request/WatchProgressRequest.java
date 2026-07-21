package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WatchProgressRequest {
    @NotNull(message = "progressSeconds wajib diisi")
    @Min(value = 0, message = "Progress tidak boleh negatif")
    private Integer progressSeconds;
    @Min(value = 0, message = "Duration tidak boleh negatif")
    private Integer durationSeconds;
    private String videoUrl;
    private String providerType;
    private String viewerHash;
}