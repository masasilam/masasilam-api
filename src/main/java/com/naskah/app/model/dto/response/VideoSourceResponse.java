package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class VideoSourceResponse {
    private Long    id;
    private String  rawUrl;
    private String  providerType;
    private String  embedUrl;
    private String  directUrl;
    private String  thumbnailUrl;
    private String  title;
    private Integer durationSeconds;
    private Boolean isTrailer;
    private Integer priority;

    /**
     * Kualitas yang tersedia (dari Archive.org, dll).
     * Key: "1080p", "720p", "480p" — Value: direct URL ke kualitas tersebut.
     * Null / kosong jika provider tidak menyediakan multi-kualitas.
     */
    private Map<String, String> qualities;
}