package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EpubStartReadingResponse {
    private boolean firstTime;
    private String lastCfi;
    private Double lastProgress;
    private Integer lastChapterIndex;
    private Integer totalChapters;
    private LocalDateTime lastReadAt;
}