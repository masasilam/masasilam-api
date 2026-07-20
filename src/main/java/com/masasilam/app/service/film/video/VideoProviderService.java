package com.masasilam.app.service.film.video;

import com.masasilam.app.model.film.FilmVideoSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProviderService {
    private final List<VideoProvider> providers;

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
                .priority(isTrailer ? 100 : 0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}