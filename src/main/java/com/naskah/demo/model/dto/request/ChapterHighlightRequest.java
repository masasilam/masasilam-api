package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterHighlightRequest {
    private Integer startPosition;
    private Integer endPosition;
    private String highlightedText;
    private String color;
    private String note;
}