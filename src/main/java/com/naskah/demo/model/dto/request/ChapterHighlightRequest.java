package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterHighlightRequest {
    private String startPosition;
    private String endPosition;
    private String highlightedText;
    private String color;
}