package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.ProjectRole;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ProjectMember {
    private Long id;
    private Long projectId;
    private Long userId;
    private ProjectRole role;
    private String assignedPages;
    private Boolean isActive;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}