package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReadingChallengeResponse {
    private Long id;
    private Long createdBy;
    private String creatorUsername;
    private String title;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String challengeType;
    private List<String> entityTypes;
    private Integer targetCount;
    private List<String> requiredGenres;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer xpReward;
    private String badgeName;
    private String badgeImageUrl;
    private Integer participantCount;
    private Integer completionCount;
    private Boolean isOfficial;
    private Boolean isActive;

    // Current user context
    private Boolean isJoined;
    private String myStatus;       // in_progress, completed, abandoned
    private Integer myProgress;
    private Double myProgressPercent;
    private List<ChallengeProgressItemResponse> myItems;

    private OffsetDateTime createdAt;
}