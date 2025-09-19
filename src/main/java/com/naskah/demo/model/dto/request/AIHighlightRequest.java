package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class AIHighlightRequest {
    private Integer startPage;
    private Integer endPage;
    private String highlightType = "IMPORTANT"; // IMPORTANT, QUOTES, DEFINITIONS, KEYWORDS
    private Integer maxHighlights = 20;
}