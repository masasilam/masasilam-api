package com.masasilam.app.model.dto.request;

import lombok.Data;

@Data
public class ChapterProgressRequest {
    private Integer position;
    private Integer readingTimeSeconds;
    private Boolean isCompleted;
}