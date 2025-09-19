package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TTSRequest {
    @NotBlank
    private String text;

    private String voice = "en-US-JennyNeural"; // Microsoft Edge voice
    private String speed = "1.0";
    private String pitch = "0Hz";
}