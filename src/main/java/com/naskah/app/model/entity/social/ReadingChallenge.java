package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReadingChallenge {
    private Long id;
    private Long createdBy;
    private String title;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String challengeType;
    private List<String> entityTypes;    // Ubah dari String ke List<String>
    private Integer targetCount;
    private List<String> requiredGenres; // Ubah dari String ke List<String>
    private Long requiredListId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer xpReward;
    private String badgeName;
    private String badgeImageUrl;
    private Integer participantCount;
    private Integer completionCount;
    private Boolean isOfficial;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}