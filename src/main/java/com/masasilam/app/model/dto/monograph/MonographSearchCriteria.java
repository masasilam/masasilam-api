package com.masasilam.app.model.dto.monograph;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonographSearchCriteria {
    private String searchQuery;
    private String sourceSlug;
    private Integer decade;
    private String status;
}