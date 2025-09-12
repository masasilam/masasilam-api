package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectActivityResponse {
    private Long id;
    private String activityType;
    private String description;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}