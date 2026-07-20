package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ZineReadingProgressRequest {

    /**
     * Posisi EPUB CFI, mis. epubcfi(/6/14!/4/2/66/1:0)
     * Dikirim dari EpubReader frontend setiap kali user berpindah halaman.
     */
    @NotBlank(message = "currentPosition (CFI) tidak boleh kosong")
    private String currentPosition;

    /** Indeks chapter saat ini (1-based), diekstrak dari CFI di frontend */
    private Integer currentPage;

    /** Total chapter di zine ini */
    @NotNull
    private Integer totalPages;

    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private BigDecimal percentageCompleted;
}