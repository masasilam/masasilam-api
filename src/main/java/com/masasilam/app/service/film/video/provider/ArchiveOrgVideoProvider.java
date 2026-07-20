package com.masasilam.app.service.film.video.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masasilam.app.service.film.video.VideoMetadata;
import com.masasilam.app.service.film.video.VideoProvider;
import com.masasilam.app.service.film.video.VideoProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ArchiveOrgVideoProvider implements VideoProvider {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Pattern ARCHIVE_PATTERN = Pattern.compile("archive\\.org/(?:details|embed|download)/([^/?&\\s]+)");
    private static final List<String> PREFERRED_FORMATS = List.of("h.264", "mpeg4", "mp4", "512kb mpeg4", "ogg video", "webm");

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("archive.org");
    }

    @Override
    public VideoMetadata resolve(String url) {
        String identifier = extractIdentifier(url);

        if (identifier == null) {
            log.warn("[Archive.org] Tidak bisa ekstrak identifier dari: {}", url);
            return VideoMetadata.builder().providerType(VideoProviderType.ARCHIVE_ORG).build();
        }

        log.debug("[Archive.org] Resolved identifier={}", identifier);

        VideoMetadata.VideoMetadataBuilder builder = VideoMetadata.builder()
                .providerType(VideoProviderType.ARCHIVE_ORG)
                .embedUrl("https://archive.org/embed/" + identifier)
                .thumbnailUrl("https://archive.org/services/img/" + identifier);

        enrichFromMetadataApi(builder, identifier);

        return builder.build();
    }

    private void enrichFromMetadataApi(VideoMetadata.VideoMetadataBuilder builder, String identifier) {
        try {
            String apiUrl = "https://archive.org/metadata/" + identifier;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Metadata Resolver)");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) return;

            JsonNode root = objectMapper.readTree(response.getBody());

            String title = root.path("metadata").path("title").asText(null);
            if (title != null && !title.isBlank()) builder.title(title);

            Map<String, String> qualities = new LinkedHashMap<>();
            String bestUrl = null;
            int bestPriority = Integer.MAX_VALUE;

            for (JsonNode file : root.path("files")) {
                String name = file.path("name").asText("");
                String fmt = file.path("format").asText("").toLowerCase();

                if (name.isBlank() || isSkippableFile(name)) continue;

                String fileUrl = "https://archive.org/download/" + identifier + "/" + name;

                int priority = rankFormat(fmt, name);
                if (priority < bestPriority) {
                    bestPriority = priority;
                    bestUrl = fileUrl;
                }

                String height = file.path("height").asText("");
                if (!height.isBlank() && !height.equals("0") && isVideoFormat(fmt, name)) {
                    String label = height + "p";
                    qualities.putIfAbsent(label, fileUrl);
                }
            }

            if (bestUrl != null) builder.directUrl(bestUrl);
            if (!qualities.isEmpty()) builder.qualities(sortQualities(qualities));

        } catch (Exception e) {
            log.warn("[Archive.org] Metadata API gagal untuk '{}': {}", identifier, e.getMessage());
        }
    }

    private boolean isSkippableFile(String name) {
        String lower = name.toLowerCase();
        return lower.contains("thumb") || lower.contains("_thumbs")
                || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".gif") || lower.endsWith(".srt")
                || lower.endsWith(".vtt") || lower.endsWith(".txt")
                || lower.endsWith(".xml") || lower.endsWith(".sqlite");
    }

    private boolean isVideoFormat(String fmt, String name) {
        return fmt.contains("h.264") || fmt.contains("mpeg4") || fmt.contains("mp4")
                || fmt.contains("webm") || fmt.contains("ogg video")
                || name.endsWith(".mp4") || name.endsWith(".webm");
    }

    private int rankFormat(String fmt, String name) {
        if (fmt.contains("h.264")) return 1;
        if (fmt.contains("mpeg4")) return 2;
        if (name.endsWith(".mp4")) return 3;
        if (fmt.contains("512kb")) return 4;
        if (fmt.contains("webm")) return 5;
        if (fmt.contains("ogg")) return 6;
        return 99;
    }

    private Map<String, String> sortQualities(Map<String, String> qualities) {
        return qualities.entrySet().stream()
                .sorted((a, b) -> {
                    int ha = parseHeight(a.getKey());
                    int hb = parseHeight(b.getKey());
                    return Integer.compare(hb, ha);
                })
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    private int parseHeight(String label) {
        try {
            return Integer.parseInt(label.replace("p", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractIdentifier(String url) {
        Matcher m = ARCHIVE_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.ARCHIVE_ORG;
    }

    @Override
    public int getOrder() {
        return 2;
    }
}