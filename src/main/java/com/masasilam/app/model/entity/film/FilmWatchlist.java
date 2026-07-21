package com.masasilam.app.model.entity.film;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmWatchlist {
    private Long id;
    private Long filmId;
    private Long userId;
    private LocalDateTime addedAt;
}