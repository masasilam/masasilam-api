package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographIndexEntry {
    private Long id;
    private Long monographId;
    private String indexType;
    private String label;
}