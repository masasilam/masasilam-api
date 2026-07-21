package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EpubBookmarkRequest {
    @NotBlank(message = "CFI tidak boleh kosong")
    @Size(max = 1000, message = "CFI terlalu panjang")
    private String cfi;
    @Size(max = 200)
    private String label;
}