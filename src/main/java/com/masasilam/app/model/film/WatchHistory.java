package com.masasilam.app.model.film;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory {
    private Long id;
    private Long filmId;
    private Long userId;
    private String viewerHash;
    private Integer progressSeconds;
    private Integer durationSeconds;
    private String providerType;
    private String videoUrl;
    private LocalDateTime lastWatchedAt;
    private Boolean completed;
}