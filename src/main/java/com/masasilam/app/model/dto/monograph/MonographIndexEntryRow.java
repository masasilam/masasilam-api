package com.masasilam.app.model.dto.monograph;

import lombok.Data;

@Data
public class MonographIndexEntryRow {
    private String label;
    private Long articleId;
    private String articleSlug;
    private String sourceSlug;
    private String title;
}