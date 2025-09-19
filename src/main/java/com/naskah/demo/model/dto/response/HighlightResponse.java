package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HighlightResponse {
    private Long id;
    private Long bookId;
    private Integer page;
    private String startPosition;
    private String endPosition;
    private String highlightedText;
    private String color;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}