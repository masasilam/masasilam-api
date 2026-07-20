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
 * Provider untuk Wikimedia Commons.
 *
 * Format URL yang didukung:
 *  - https://commons.wikimedia.org/wiki/File:Something.webm
 *  - https://upload.wikimedia.org/wikipedia/commons/a/ab/Something.webm
 *
 * Wikimedia hanya menyediakan direct URL (WebM/OGV/MP4).
 * Tidak ada iframe embed standar — frontend memutar langsung via <video> tag.
 */
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
        return url != null
                && (url.contains("commons.wikimedia.org") || url.contains("upload.wikimedia.org"));
    }

    @Override
    public VideoMetadata resolve(String url) {
        String directUrl  = null;
        String embedUrl   = null;
        String filename   = null;

        // Jika URL sudah berupa direct upload link
        Matcher uploadMatcher = UPLOAD_PATTERN.matcher(url);
        if (uploadMatcher.find()) {
            directUrl = uploadMatcher.group(1);
        }

        // Ekstrak filename untuk Special:FilePath
        Matcher fileMatcher = FILE_PATTERN.matcher(url);
        if (fileMatcher.find()) {
            filename = fileMatcher.group(1);
            embedUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
            if (directUrl == null) {
                // Bangun direct URL via upload.wikimedia.org
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

    /**
     * Bangun URL upload Wikimedia menggunakan MD5 hash dari filename.
     * Format: upload.wikimedia.org/wikipedia/commons/{a}/{ab}/{filename}
     */
    private String buildDirectUrl(String filename) {
        try {
            String fn = filename.replace(" ", "_");
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(fn.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String md5 = String.format("%02x%02x", hash[0], hash[1]);
            return "https://upload.wikimedia.org/wikipedia/commons/"
                    + md5.charAt(0) + "/" + md5 + "/" + fn;
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