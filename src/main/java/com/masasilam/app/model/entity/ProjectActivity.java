package com.masasilam.app.model.entity;

import com.masasilam.app.model.enums.ProjectActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectActivity {
    private Long id;
    private Long projectId;
    private Long userId;
    private ProjectActivityType activityType;
    private String description;
    private Integer pageNumber;
    private String metadata;
    private LocalDateTime createdAt;
}