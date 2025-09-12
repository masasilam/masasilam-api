package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserActivity {
    private Long id;
    private Long userId;
    private String activityType;
    private String entityType;
    private Long entityId;
    private String metadata;
    private String status;
    private LocalDateTime createdAt;
}