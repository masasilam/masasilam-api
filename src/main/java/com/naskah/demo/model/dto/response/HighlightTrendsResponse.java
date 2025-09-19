package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class HighlightTrendsResponse {
    private String bookTitle;
    private Integer totalHighlights;
    private List<TrendingHighlight> trendingHighlights;
    private List<PopularPage> popularPages;

    @Data
    public static class TrendingHighlight {
        private String text;
        private Integer highlightCount;
        private Integer page;
        private Double trendScore;
    }

    @Data
    public static class PopularPage {
        private Integer page;
        private Integer highlightCount;
        private String mostHighlightedText;
    }
}