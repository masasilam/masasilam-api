package com.naskah.app.service.film.video.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.app.service.film.video.VideoMetadata;
import com.naskah.app.service.film.video.VideoProvider;
import com.naskah.app.service.film.video.VideoProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider untuk platform yang mendukung oEmbed standard:
 *  - Vimeo (vimeo.com)
 *  - Dailymotion (dailymotion.com)
 *
 * Cara kerja:
 *  1. Ekstrak video ID dari URL secara langsung (cepat, tanpa HTTP call)
 *  2. Bangun embed URL dari template
 *  3. Enrich metadata (judul, thumbnail, durasi) dari oEmbed API
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class OEmbedVideoProvider implements VideoProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Konfigurasi per provider: domain → (oEmbed endpoint, ID regex, embed template)
    private record ProviderConfig(
            String oEmbedEndpoint,
            Pattern idPattern,
            String embedTemplate,
            VideoProviderType type
    ) {}

    private static final Map<String, ProviderConfig> PROVIDERS = new LinkedHashMap<>();

    static {
        PROVIDERS.put("vimeo.com", new ProviderConfig(
                "https://vimeo.com/api/oembed.json?url=",
                Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)"),
                "https://player.vimeo.com/video/%s?dnt=1&color=ffffff",
                VideoProviderType.VIMEO
        ));

        PROVIDERS.put("dailymotion.com", new ProviderConfig(
                "https://www.dailymotion.com/services/oembed?url=",
                Pattern.compile("dailymotion\\.com/(?:video|embed/video)/([a-z0-9]+)"),
                "https://www.dailymotion.com/embed/video/%s",
                VideoProviderType.DAILYMOTION
        ));
    }

    @Override
    public boolean supports(String url) {
        if (url == null || url.isBlank()) return false;
        return PROVIDERS.keySet().stream().anyMatch(url::contains);
    }

    @Override
    public VideoMetadata resolve(String url) {
        // Cari config yang cocok
        Map.Entry<String, ProviderConfig> match = PROVIDERS.entrySet().stream()
                .filter(e -> url.contains(e.getKey()))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return VideoMetadata.builder().providerType(VideoProviderType.VIMEO).build();
        }

        ProviderConfig cfg = match.getValue();

        VideoMetadata.VideoMetadataBuilder builder = VideoMetadata.builder()
                .providerType(cfg.type());

        // Step 1: Bangun embed URL dari ID (tanpa HTTP call)
        Matcher m = cfg.idPattern().matcher(url);
        if (m.find()) {
            String videoId = m.group(1);
            builder.embedUrl(String.format(cfg.embedTemplate(), videoId));
            log.debug("[{}] videoId={}, embedUrl built from template", cfg.type(), videoId);
        } else {
            log.warn("[{}] Tidak bisa ekstrak video ID dari: {}", cfg.type(), url);
        }

        // Step 2: Enrich dengan oEmbed API
        enrichFromOEmbed(builder, url, cfg);

        return builder.build();
    }

    private void enrichFromOEmbed(VideoMetadata.VideoMetadataBuilder builder,
                                  String url, ProviderConfig cfg) {
        try {
            String oEmbedUrl = cfg.oEmbedEndpoint()
                    + URLEncoder.encode(url, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    oEmbedUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) return;

            JsonNode data = objectMapper.readTree(response.getBody());

            String title     = data.path("title").asText(null);
            String thumbnail = data.path("thumbnail_url").asText(null);
            int    duration  = data.path("duration").asInt(0);

            if (title     != null && !title.isBlank())     builder.title(title);
            if (thumbnail != null && !thumbnail.isBlank()) builder.thumbnailUrl(thumbnail);
            if (duration  > 0)                              builder.durationSeconds(duration);

        } catch (Exception e) {
            log.warn("[{}] oEmbed gagal untuk '{}': {}", cfg.type(), url, e.getMessage());
        }
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.VIMEO; // primary type; dailymotion ditangani juga
    }

    @Override
    public int getOrder() {
        return 3;
    }
}