package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProgressRequest {
    @NotNull
    @Min(1)
    private Integer currentPage;

    @NotNull
    private String currentPosition;

    @Min(0)
    @Max(100)
    private BigDecimal percentageCompleted;

    @Min(0)
    private Integer readingTimeMinutes;

    private String status; // READING, COMPLETED, PAUSED
}
