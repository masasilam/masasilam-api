package com.masasilam.app.model.dto.response;

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
        private Integer frequency;
        private Integer page;
    }
}