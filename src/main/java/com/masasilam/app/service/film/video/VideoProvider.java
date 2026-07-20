package com.masasilam.app.service.film.video;

public interface VideoProvider {
    boolean supports(String url);
    VideoMetadata resolve(String url);
    VideoProviderType getType();
    int getOrder();
}