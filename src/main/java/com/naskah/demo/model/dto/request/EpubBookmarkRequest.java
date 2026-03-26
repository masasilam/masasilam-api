package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EpubBookmarkRequest {

    @NotBlank(message = "CFI tidak boleh kosong")
    @Size(max = 1000, message = "CFI terlalu panjang")
    private String cfi;

    /** Label singkat ditampilkan di sidebar, contoh: "Halaman 12" atau "Posisi 34%" */
    @Size(max = 200)
    private String label;
}