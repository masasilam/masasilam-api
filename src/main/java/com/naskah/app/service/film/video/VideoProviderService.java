package com.naskah.app.service.film.video;

import com.naskah.app.model.film.FilmVideoSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Router utama untuk sistem video provider.
 *
 * Spring Boot akan meng-inject semua implementasi VideoProvider secara otomatis.
 * Setiap URL video dicocokkan ke provider yang tepat berdasarkan urutan priority (getOrder()).
 *
 * Cara menambah provider baru:
 *   1. Buat class implements VideoProvider
 *   2. Beri @Component dan @Order(N)
 *   3. Service ini otomatis mendeteksi provider baru — zero config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProviderService {

    /** Spring inject semua @Component yang implements VideoProvider */
    private final List<VideoProvider> providers;

    /**
     * Resolve URL video ke metadata lengkap.
     * Provider dicek berurutan berdasarkan getOrder() — paling kecil paling dulu.
     *
     * @param url URL video mentah (YouTube, archive.org, .m3u8, .mp4, dll)
     * @return VideoMetadata, atau null jika URL kosong
     */
    public VideoMetadata resolve(String url) {
        if (url == null || url.isBlank()) return null;

        return providers.stream()
                .sorted(Comparator.comparingInt(VideoProvider::getOrder))
                .filter(p -> p.supports(url))
                .findFirst()
                .map(p -> {
                    log.info("[VideoProvider] '{}' → {}", p.getType(), url);
                    return p.resolve(url);
                })
                .orElseGet(() -> {
                    log.warn("[VideoProvider] Tidak ada provider cocok untuk: {} — fallback UNKNOWN", url);
                    return VideoMetadata.builder()
                            .providerType(VideoProviderType.UNKNOWN)
                            .directUrl(url)
                            .build();
                });
    }

    /**
     * Resolve URL dan langsung konversi menjadi entity FilmVideoSource siap simpan ke DB.
     *
     * @param url       URL video mentah
     * @param filmId    ID film yang terkait
     * @param isTrailer apakah video ini adalah trailer
     * @return FilmVideoSource entity (belum di-insert)
     */
    public FilmVideoSource resolveToEntity(String url, Long filmId, boolean isTrailer) {
        VideoMetadata meta = resolve(url);
        if (meta == null) return null;

        return FilmVideoSource.builder()
                .filmId(filmId)
                .rawUrl(url)
                .providerType(meta.getProviderType().name())
                .embedUrl(meta.getEmbedUrl())
                .directUrl(meta.getDirectUrl())
                .thumbnailUrl(meta.getThumbnailUrl())
                .title(meta.getTitle())
                .durationSeconds(meta.getDurationSeconds())
                .isTrailer(isTrailer)
                .isActive(true)
                .priority(isTrailer ? 100 : 0) // trailer punya prioritas lebih tinggi
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Deteksi tipe provider dari URL tanpa resolve penuh (cepat, tidak ada HTTP call).
     *
     * @param url URL video mentah
     * @return VideoProviderType
     */
    public VideoProviderType detectType(String url) {
        if (url == null || url.isBlank()) return VideoProviderType.UNKNOWN;

        return providers.stream()
                .sorted(Comparator.comparingInt(VideoProvider::getOrder))
                .filter(p -> p.supports(url))
                .map(VideoProvider::getType)
                .findFirst()
                .orElse(VideoProviderType.UNKNOWN);
    }
}