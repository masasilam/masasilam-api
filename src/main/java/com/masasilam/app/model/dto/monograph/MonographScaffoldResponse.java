package com.masasilam.app.model.dto.monograph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographScaffoldResponse {
    private String suggestedTitle;
    private LocalDate editionDate;
    private LocalDate editionDateTo;
    private Long sourceId;
    private String sourceName;
    private Integer totalCandidateArticles;
    private Integer totalWordCountEstimate;
    private List<MonographScaffoldArticle> candidateArticles;
}