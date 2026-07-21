package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EpubAnnotationRequest {
    @NotBlank(message = "CFI tidak boleh kosong")
    @Size(max = 1000, message = "CFI terlalu panjang")
    private String cfi;
    @NotBlank(message = "Teks yang dipilih tidak boleh kosong")
    @Size(max = 5000, message = "Teks terlalu panjang")
    private String selectedText;
    @Size(max = 20)
    private String color;
    @Size(max = 2000, message = "Catatan terlalu panjang")
    private String note;
}