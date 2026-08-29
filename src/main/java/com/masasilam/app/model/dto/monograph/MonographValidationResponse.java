package com.masasilam.app.model.dto.monograph;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MonographValidationResponse {
    private boolean ready;
    private List<String> issues;
}