package com.masasilam.app.model.dto.response.social;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class NotificationResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderPhoto;
    private String type;
    private String entityType;
    private Long entityId;
    private String message;
    private Map<String, Object> data;
    private Boolean isRead;
    private OffsetDateTime createdAt;
    private String timeAgo;
}