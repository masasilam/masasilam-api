package com.naskah.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddVideoSourceRequest {

    @NotBlank(message = "URL video tidak boleh kosong")
    @Size(max = 1000, message = "URL maksimal 1000 karakter")
    private String url;

    /** true = trailer, false = full movie */
    private boolean isTrailer = false;

    /**
     * Prioritas urutan tampil.
     * Makin besar = makin diutamakan sebagai video utama.
     * Default 0; trailer biasanya diberi nilai 100 oleh sistem.
     */
    private Integer priority = 0;
}