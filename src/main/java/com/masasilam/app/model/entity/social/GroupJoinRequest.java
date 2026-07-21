package com.masasilam.app.model.entity.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupJoinRequest {
    private Long id;
    private Long groupId;
    private Long userId;
    private String message;
    private String status;
    private Long reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}