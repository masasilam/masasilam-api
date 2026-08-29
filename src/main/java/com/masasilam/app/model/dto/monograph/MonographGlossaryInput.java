package com.masasilam.app.model.dto.monograph;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MonographGlossaryInput {
    @NotBlank
    private String term;
    @NotBlank
    private String definition;
    private Integer order;
}