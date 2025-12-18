package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AchievementResponse {
    private String achievementId;
    private String category;                // "reading", "social", "contribution"
    private String title;
    private String description;
    private String badgeUrl;
    private Integer points;

    private Boolean isUnlocked;
    private LocalDateTime unlockedAt;

    private Integer currentProgress;
    private Integer targetProgress;
    private Double progressPercentage;

    private String tier;                    // "bronze", "silver", "gold"
    private Integer rarityPercentage;       // How many users have this
}
