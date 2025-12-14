package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterBookmarkRequest {
    private Integer position; // Position in chapter
    private String title;
    private String description;
    private String color;
}