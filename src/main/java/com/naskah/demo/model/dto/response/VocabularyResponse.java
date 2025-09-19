package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class VocabularyResponse {
    private Integer totalWords;
    private String difficultyLevel;
    private List<VocabularyWord> words;

    @Data
    public static class VocabularyWord {
        private String word;
        private String definition;
        private String pronunciation;
        private String partOfSpeech;
        private List<String> examples;
        private String difficulty;
        private Integer frequency; // How often it appears in the book
        private Integer page; // First occurrence
    }
}