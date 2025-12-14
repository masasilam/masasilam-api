package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterProgressRequest {
    private Integer position; // Current scroll position or percentage
    private Integer readingTimeSeconds;
    private Boolean isCompleted;
}