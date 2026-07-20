package com.naskah.app.service.film.video.provider;

import com.naskah.app.service.film.video.VideoMetadata;
import com.naskah.app.service.film.video.VideoProvider;
import com.naskah.app.service.film.video.VideoProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Provider fallback untuk URL video langsung (MP4, WebM, dll).
 *
 * Dieksekusi terakhir (order=99) — hanya jika tidak ada provider lain yang cocok.
 * Mendukung file video yang dihost di server manapun selama ekstensinya dikenali.
 *
 * Frontend memutar langsung via tag <video>.
 */
@Slf4j
@Component
@Order(99)
public class DirectUrlVideoProvider implements VideoProvider {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".ogv", ".ogg", ".avi", ".mov", ".mkv", ".flv", ".wmv"
    );

    @Override
    public boolean supports(String url) {
        if (url == null || url.isBlank()) return false;
        // Strip query params dan fragment sebelum cek ekstensi
        String path = url.toLowerCase().split("[?#]")[0];
        return VIDEO_EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    @Override
    public VideoMetadata resolve(String url) {
        log.debug("[DirectUrl] Resolved as direct video: {}", url);

        return VideoMetadata.builder()
                .providerType(VideoProviderType.DIRECT_URL)
                .directUrl(url)
                .embedUrl(null)
                .supportsHls(false)
                .build();
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.DIRECT_URL;
    }

    @Override
    public int getOrder() {
        return 99;
    }
}