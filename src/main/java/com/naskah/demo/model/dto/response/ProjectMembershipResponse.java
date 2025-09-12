package com.naskah.demo.model.dto.response;

import com.naskah.demo.model.enums.ProjectStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProjectMembershipResponse {
    private Long projectId;
    private String projectTitle;
    private String projectSlug;
    private String role;
    private String assignedPages;
    private LocalDateTime joinedAt;
    private BigDecimal projectProgress;
    private ProjectStatus projectStatus;
    private Integer myCompletedPages;
    private LocalDateTime lastActivity;
}