package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizRequest {
    @NotNull
    @Min(1)
    private Integer chapter;

    private Integer questionCount = 5;
    private String difficulty = "MEDIUM"; // EASY, MEDIUM, HARD
    private String quizType = "MULTIPLE_CHOICE"; // MULTIPLE_CHOICE, TRUE_FALSE, OPEN_ENDED
}