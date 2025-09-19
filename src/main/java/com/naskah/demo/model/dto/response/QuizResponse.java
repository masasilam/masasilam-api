package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class QuizResponse {
    private Integer chapter;
    private String chapterTitle;
    private String difficulty;
    private List<QuizQuestion> questions;
    private Integer totalQuestions;

    @Data
    public static class QuizQuestion {
        private Long id;
        private String question;
        private String type; // MULTIPLE_CHOICE, TRUE_FALSE, OPEN_ENDED
        private List<String> options; // For multiple choice
        private String correctAnswer;
        private String explanation;
        private Integer page; // Reference page
    }
}