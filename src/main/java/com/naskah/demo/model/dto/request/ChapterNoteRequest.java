package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterNoteRequest {
    private Integer startPosition;
    private Integer endPosition;
    private String content;
    private String selectedText;
}