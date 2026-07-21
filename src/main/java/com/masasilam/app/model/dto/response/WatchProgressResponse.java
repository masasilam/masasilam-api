package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class WatchProgressResponse {
    private Long filmId;
    private Integer progressSeconds;
    private Integer durationSeconds;
    private Double percentage;
    private Boolean completed;
    private String providerType;
    private String videoUrl;
}