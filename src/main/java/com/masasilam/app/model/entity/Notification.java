package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long userId;
    private String message;
    private NotificationType type;
    private Long projectId;
    private Integer pageNumber;
    private Long relatedEntityId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}