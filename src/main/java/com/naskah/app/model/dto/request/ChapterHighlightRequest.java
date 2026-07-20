package com.naskah.app.model.dto.request;

import lombok.Data;

@Data
public class ChapterHighlightRequest {
    private Integer startPosition;
    private Integer endPosition;
    private String highlightedText;
    private String color;
}