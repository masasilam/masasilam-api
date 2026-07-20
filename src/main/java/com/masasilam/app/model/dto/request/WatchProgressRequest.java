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

    /** URL video yang sedang diputar (untuk mengetahui sumber mana yang ditonton) */
    private String videoUrl;

    /** Tipe provider: YOUTUBE, ARCHIVE_ORG, dll */
    private String providerType;

    /**
     * Hash unik untuk guest user (tidak login).
     * Frontend harus generate dan simpan di localStorage.
     * Format: SHA-256 dari kombinasi userAgent + IP (di-generate server saat getMyProgress).
     */
    private String viewerHash;
}