package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitCorrectionRequest {
    @NotBlank(message = "Teks asli tidak boleh kosong")
    @Size(max = 500, message = "Teks asli maksimal 500 karakter")
    private String originalText;
    @NotBlank(message = "Teks koreksi tidak boleh kosong")
    @Size(max = 500, message = "Teks koreksi maksimal 500 karakter")
    private String correctedText;
    @Size(max = 100)
    private String contextBefore;
    @Size(max = 100)
    private String contextAfter;
    private String epubCfi;
    private String sectionHref;
    private Integer startPosition;
    private Integer endPosition;
    @Size(max = 500, message = "Catatan maksimal 500 karakter")
    private String userNote;
}