package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterAudioRequest {
    private String voice; // Voice ID
    private Double speed; // 0.5 - 2.0
    private String format; // mp3, m4a
}