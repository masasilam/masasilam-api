package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QAResponse {
    private String question;
    private String answer;
    private List<Reference> references;
    private Double confidenceScore;
    private LocalDateTime answeredAt;
    private String aiProvider = "OpenAI GPT";

    @Data
    public static class Reference {
        private Integer page;
        private String text;
        private String context;
    }
}