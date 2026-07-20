package com.naskah.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FilmWatchlistResponse {
    private Long          id;
    private Long          filmId;
    private String        filmSlug;
    private String        filmTitle;
    private String        posterUrl;
    private String        tahunRilis;
    private LocalDateTime addedAt;
}