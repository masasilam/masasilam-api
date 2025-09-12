package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.ProjectActivityType;
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