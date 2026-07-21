package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FilmRatingResponse {
    private Long id;
    private Long filmId;
    private Long userId;
    private String username;
    private String userPhotoUrl;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}