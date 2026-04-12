package com.naskah.demo.model.dto.response;

import lombok.Data;

// Response DTO baru
@Data
public class EpubStartReadingResponse {
    private boolean firstTime;      // true = baca pertama kali
    private String  lastCfi;        // null jika firstTime
    private Double  lastProgress;   // null jika firstTime
    private Integer lastChapterIndex;
    private String  lastChapterLabel;
    private Integer totalChapters;
}