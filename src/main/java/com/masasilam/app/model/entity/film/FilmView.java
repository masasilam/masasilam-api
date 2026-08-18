package com.masasilam.app.model.entity.film;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FilmView {
    private Long id;
    private Long filmId;
    private String slug;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
    private String actionType;
    private LocalDateTime viewedAt;
}