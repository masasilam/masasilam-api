package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SummaryResponse {
    private Integer chapter;
    private String chapterTitle;
    private String summary;
    private String summaryType;
    private List<String> keyPoints;
    private Integer wordCount;
    private LocalDateTime generatedAt;
    private String aiProvider = "OpenAI GPT";
}