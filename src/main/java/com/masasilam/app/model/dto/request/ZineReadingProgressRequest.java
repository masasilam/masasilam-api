package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ZineReadingProgressRequest {
    @NotBlank(message = "currentPosition (CFI) tidak boleh kosong")
    private String currentPosition;
    private Integer currentPage;
    @NotNull
    private Integer totalPages;
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal percentageCompleted;
}