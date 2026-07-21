package com.masasilam.app.model.entity.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocialNotification {
    private Long id;
    private Long recipientId;
    private Long senderId;
    private String type;
    private String entityType;
    private Long entityId;
    private String message;
    private String data;
    private Boolean isRead;
    private LocalDateTime createdAt;
}