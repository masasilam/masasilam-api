package com.naskah.demo.model.dto.response;

import com.naskah.demo.model.enums.ProjectRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectMemberResponse {
    private Long id;
    private Long userId;
    private ProjectRole role;
    private List<Integer> assignedPageNumbers;
    private Boolean isActive;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    // Progress information
    private Integer completedPages;
    private Integer totalAssignedPages;
    private Double completionPercentage;
}
