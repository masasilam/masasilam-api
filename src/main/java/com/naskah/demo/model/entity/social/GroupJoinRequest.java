package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupJoinRequest {
    private Long id;
    private Long groupId;
    private Long userId;
    private String message;
    private String status;  // pending, approved, rejected
    private Long reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}