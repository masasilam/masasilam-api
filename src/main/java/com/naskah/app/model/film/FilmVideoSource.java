package com.naskah.app.model.film;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmVideoSource {
    private Long          id;
    private Long          filmId;
    private String        rawUrl;           // URL asli yang dimasukkan admin
    private String        providerType;     // YOUTUBE, ARCHIVE_ORG, VIMEO, HLS, dll
    private String        embedUrl;         // URL untuk iframe embed
    private String        directUrl;        // URL file video langsung (jika ada)
    private String        thumbnailUrl;     // Thumbnail video
    private String        title;            // Judul video dari provider
    private Integer       durationSeconds;  // Durasi dalam detik
    private Boolean       isTrailer;        // true = trailer, false = full movie
    private Boolean       isActive;         // soft disable tanpa hapus
    private Integer       priority;         // urutan tampil (makin besar = makin utama)
    private LocalDateTime createdAt;
}