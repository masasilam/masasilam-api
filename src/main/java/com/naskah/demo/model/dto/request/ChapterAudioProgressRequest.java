package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterAudioProgressRequest {
    private Long position; // seconds
    private Boolean isCompleted;
}
