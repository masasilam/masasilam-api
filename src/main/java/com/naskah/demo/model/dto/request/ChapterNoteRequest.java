package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterNoteRequest {
    private String position;
    private String content;
    private String selectedText;
}