package com.masasilam.app.model.film;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmVideoSource {
    private Long id;
    private Long filmId;
    private String rawUrl;
    private String providerType;
    private String embedUrl;
    private String directUrl;
    private String thumbnailUrl;
    private String title;
    private Integer durationSeconds;
    private Boolean isTrailer;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime createdAt;
}