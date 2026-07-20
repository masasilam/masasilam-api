package com.masasilam.app.service.film.video.provider;

import com.masasilam.app.service.film.video.VideoMetadata;
import com.masasilam.app.service.film.video.VideoProvider;
import com.masasilam.app.service.film.video.VideoProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(4)
public class WikimediaVideoProvider implements VideoProvider {
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "(?:commons\\.wikimedia\\.org/wiki/File:|upload\\.wikimedia\\.org/.+?/)([^/?#]+\\.(?:webm|ogv|ogg|mp4))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UPLOAD_PATTERN = Pattern.compile(
            "(https?://upload\\.wikimedia\\.org/[^\"\\s]+\\.(?:webm|ogv|ogg|mp4))",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean supports(String url) {
        return url != null && (url.contains("commons.wikimedia.org") || url.contains("upload.wikimedia.org"));
    }

    @Override
    public VideoMetadata resolve(String url) {
        String directUrl = null;
        String embedUrl = null;
        String filename = null;

        Matcher uploadMatcher = UPLOAD_PATTERN.matcher(url);
        if (uploadMatcher.find()) {
            directUrl = uploadMatcher.group(1);
        }

        Matcher fileMatcher = FILE_PATTERN.matcher(url);
        if (fileMatcher.find()) {
            filename = fileMatcher.group(1);
            embedUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
            if (directUrl == null) {
                directUrl = buildDirectUrl(filename);
            }
        }

        log.debug("[Wikimedia] filename={}, directUrl={}", filename, directUrl);

        return VideoMetadata.builder()
                .providerType(VideoProviderType.WIKIMEDIA)
                .directUrl(directUrl)
                .embedUrl(embedUrl)
                .supportsHls(false)
                .build();
    }

    private String buildDirectUrl(String filename) {
        try {
            String fn = filename.replace(" ", "_");
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(fn.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String md5 = String.format("%02x%02x", hash[0], hash[1]);
            return "https://upload.wikimedia.org/wikipedia/commons/" + md5.charAt(0) + "/" + md5 + "/" + fn;
        } catch (Exception e) {
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
        }
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.WIKIMEDIA;
    }

    @Override
    public int getOrder() {
        return 4;
    }
}