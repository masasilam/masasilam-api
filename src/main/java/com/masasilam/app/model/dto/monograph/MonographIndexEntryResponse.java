package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.util.List;

@Data
public class MonographIndexEntryResponse {
    private String label;
    private List<MonographIndexRefResponse> references;
}