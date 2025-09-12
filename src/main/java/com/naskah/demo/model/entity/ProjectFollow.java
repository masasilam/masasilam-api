package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectFollow {
    private Long id;
    private Long projectId;
    private Long userId;
    private LocalDateTime followedAt;
    private Boolean isActive;
}