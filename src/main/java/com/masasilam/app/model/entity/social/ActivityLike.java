package com.masasilam.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityLike {
    private Long id;
    private Long activityId;
    private Long userId;
    private LocalDateTime createdAt;
}