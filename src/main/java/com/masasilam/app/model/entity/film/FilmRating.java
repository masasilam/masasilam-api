package com.masasilam.app.model.entity.film;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmRating {
    private Long id;
    private Long filmId;
    private Long userId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}