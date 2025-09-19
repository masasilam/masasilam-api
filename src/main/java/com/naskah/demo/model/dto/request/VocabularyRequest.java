package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class VocabularyRequest {
    private Integer startPage;
    private Integer endPage;
    private String difficultyLevel; // BEGINNER, INTERMEDIATE, ADVANCED
    private Integer maxWords = 50;
}