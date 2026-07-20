package com.naskah.app.model.dto.response;

import lombok.Data;

@Data
public class WatchProgressResponse {
    private Long    filmId;
    private Integer progressSeconds;   // posisi terakhir
    private Integer durationSeconds;   // total durasi
    private Double  percentage;        // persentase progress (0.0 - 100.0)
    private Boolean completed;         // true jika sudah nonton >= 90%
    private String  providerType;      // YOUTUBE, ARCHIVE_ORG, dll
    private String  videoUrl;          // URL video yang sedang/terakhir ditonton
}