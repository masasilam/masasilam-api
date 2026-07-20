package com.masasilam.app.model.film;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory {
    private Long          id;
    private Long          filmId;
    private Long          userId;           // null untuk guest
    private String        viewerHash;       // fingerprint untuk guest
    private Integer       progressSeconds;  // posisi terakhir (detik)
    private Integer       durationSeconds;  // total durasi video
    private String        providerType;     // YOUTUBE, ARCHIVE_ORG, dll
    private String        videoUrl;         // URL video yang sedang ditonton
    private LocalDateTime lastWatchedAt;
    private Boolean       completed;        // true jika sudah nonton >= 90%
}