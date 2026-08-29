package com.masasilam.app.model.dto.monograph;

import lombok.Data;

@Data
public class MonographCriticalNoteResponse {
    private Integer number;
    private String noteText;
    private Long relatedArticleId;
    private String relatedArticleSlug;
    private String relatedArticleSourceSlug;
}