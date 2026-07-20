package com.masasilam.app.service.film.video;

import lombok.*;
import java.util.Map;

/**
 * Hasil resolusi URL video dari salah satu VideoProvider.
 * Objek ini bersifat immutable setelah dibangun.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {

    /** URL untuk iframe embed (null jika provider tidak mendukung embed) */
    private String embedUrl;

    /** URL langsung ke file video (null jika provider tidak mengizinkan) */
    private String directUrl;

    /** Thumbnail / poster frame video */
    private String thumbnailUrl;

    /** Judul video dari provider */
    private String title;

    /** Durasi video dalam detik (null jika tidak tersedia) */
    private Integer durationSeconds;

    /** Tipe provider yang berhasil menangani URL ini */
    private VideoProviderType providerType;

    /**
     * true jika URL ini adalah HLS/DASH stream.
     * Frontend perlu menggunakan hls.js atau dash.js.
     */
    private boolean supportsHls;

    /**
     * Peta kualitas video yang tersedia.
     * Key: label kualitas (misal "1080p", "720p", "MP4")
     * Value: direct URL ke file kualitas tersebut
     * Kosong jika provider tidak menyediakan banyak kualitas.
     */
    private Map<String, String> qualities;
}