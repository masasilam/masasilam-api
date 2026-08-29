package com.masasilam.app.model.dto.monograph;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MonographAdjacentResponse {
    private Long currentMonographId;
    private String newspaperSourceName;
    private String newspaperSourceSlug;
    private MonographNeighbor previousMonograph;
    private MonographNeighbor nextMonograph;

    @Data
    public static class MonographNeighbor {
        private Long id;
        private String slug;
        private String title;
        private LocalDate editionDate;
        private String editionDateFormatted;
        private String coverImageUrl;
    }
}