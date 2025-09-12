package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String message;
    private String type;
    private Long projectId;
    private String projectTitle;
    private Integer pageNumber;
    private Boolean isRead;
    private LocalDateTime createdAt;
}