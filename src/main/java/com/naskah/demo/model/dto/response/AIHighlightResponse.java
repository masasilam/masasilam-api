package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AIHighlightResponse {
    private String highlightType;
    private Integer totalHighlights;
    private List<AIHighlight> highlights;

    @Data
    public static class AIHighlight {
        private Integer page;
        private String text;
        private String startPosition;
        private String endPosition;
        private String category; // IMPORTANT, QUOTE, DEFINITION, etc.
        private Double confidenceScore;
        private String reason;
    }
}