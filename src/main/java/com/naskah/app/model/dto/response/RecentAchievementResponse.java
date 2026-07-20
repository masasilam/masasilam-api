package com.naskah.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentAchievementResponse {
    private String        achievementId;
    private String        title;
    private String        description;
    private String        badgeUrl;
    private LocalDateTime unlockedAt;
}
