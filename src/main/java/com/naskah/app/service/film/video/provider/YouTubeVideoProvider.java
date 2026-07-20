package com.naskah.app.service.film.video.provider;

import com.naskah.app.service.film.video.VideoMetadata;
import com.naskah.app.service.film.video.VideoProvider;
import com.naskah.app.service.film.video.VideoProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider untuk YouTube.
 * Mendukung format URL:
 *  - https://www.youtube.com/watch?v=dQw4w9WgXcQ
 *  - https://youtu.be/dQw4w9WgXcQ
 *  - https://www.youtube.com/embed/dQw4w9WgXcQ
 *  - https://www.youtube.com/shorts/dQw4w9WgXcQ
 *
 * YouTube tidak mengizinkan direct download, sehingga hanya embedUrl yang tersedia.
 * Menggunakan youtube-nocookie.com agar tidak meninggalkan cookie tracking pada viewer.
 */
@Slf4j
@Component
@Order(1)
public class YouTubeVideoProvider implements VideoProvider {

    private static final Pattern YT_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/)|youtu\\.be/)([\\w-]{11})"
    );

    @Override
    public boolean supports(String url) {
        if (url == null || url.isBlank()) return false;
        return YT_PATTERN.matcher(url).find();
    }

    @Override
    public VideoMetadata resolve(String url) {
        String videoId = extractVideoId(url);

        if (videoId == null) {
            log.warn("[YouTube] Tidak bisa ekstrak video ID dari: {}", url);
            return VideoMetadata.builder().providerType(VideoProviderType.YOUTUBE).build();
        }

        log.debug("[YouTube] Resolved videoId={}", videoId);

        return VideoMetadata.builder()
                .providerType(VideoProviderType.YOUTUBE)
                .embedUrl(buildEmbedUrl(videoId))
                .directUrl(null)   // YouTube tidak izinkan direct access
                .thumbnailUrl("https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg")
                .supportsHls(false)
                .build();
    }

    private String buildEmbedUrl(String videoId) {
        return "https://www.youtube-nocookie.com/embed/" + videoId
                + "?rel=0&modestbranding=1&playsinline=1";
    }

    private String extractVideoId(String url) {
        Matcher m = YT_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.YOUTUBE;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}