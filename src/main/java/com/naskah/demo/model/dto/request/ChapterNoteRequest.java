package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterNoteRequest {
    private Integer position;
    private String title;
    private String content;
    private String color;
    private Boolean isPrivate;
}