package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TaggingResponse {
    private Integer processedNotes;
    private List<String> generatedTags;
    private Map<Long, List<String>> noteTagMapping; // noteId -> tags
    private List<TagSuggestion> suggestions;

    @Data
    public static class TagSuggestion {
        private String tag;
        private Integer frequency;
        private String category;
    }
}