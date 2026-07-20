package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FilmReviewRequest {

    @Size(max = 200, message = "Judul ulasan maksimal 200 karakter")
    private String title;

    @NotBlank(message = "Konten ulasan tidak boleh kosong")
    @Size(min = 10, max = 10000, message = "Konten ulasan antara 10 sampai 10.000 karakter")
    private String content;
}