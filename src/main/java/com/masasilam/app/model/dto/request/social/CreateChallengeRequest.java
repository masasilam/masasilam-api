package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateChallengeRequest {
    @NotBlank
    private String title;

    private String description;
    private String coverImageUrl;

    @NotNull
    private String challengeType; // COUNT_BASED, GENRE_BASED, DATE_BASED, LIST_BASED

    private List<String> entityTypes;  // BOOK, ZINE, FILM, etc.
    private Integer targetCount;
    private List<String> requiredGenres;
    private Long requiredListId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer xpReward;
    private String badgeName;
    private String badgeImageUrl;
}