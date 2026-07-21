package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class VideoSourceResponse {
    private Long id;
    private String rawUrl;
    private String providerType;
    private String embedUrl;
    private String directUrl;
    private String thumbnailUrl;
    private String title;
    private Integer durationSeconds;
    private Boolean isTrailer;
    private Integer priority;
    private Map<String, String> qualities;
}