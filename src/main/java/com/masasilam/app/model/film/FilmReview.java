package com.masasilam.app.model.film;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmReview {
    private Long          id;
    private Long          filmId;
    private Long          userId;
    private String        title;
    private String        content;
    private Integer       helpfulCount;
    private Integer       notHelpfulCount;
    private Integer       replyCount;
    private Boolean       isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}