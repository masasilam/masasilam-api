package com.masasilam.app.service.film.video;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    private String embedUrl;
    private String directUrl;
    private String thumbnailUrl;
    private String title;
    private Integer durationSeconds;
    private VideoProviderType providerType;
    private boolean supportsHls;
    private Map<String, String> qualities;
}