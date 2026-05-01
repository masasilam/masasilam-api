package com.naskah.demo.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SetCurrentReadRequest {
    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;

    private LocalDate startDate;
    private LocalDate endDate;
}