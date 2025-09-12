package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectQualityAssessment {
    private Long id;
    private Long projectId;
    private Long assessorId;
    private Integer qualityScore;
    private Integer contentAccuracy;
    private Integer languageQuality;
    private Integer formattingQuality;
    private Integer overallReadability;
    private String recommendations;
    private LocalDateTime createdAt;
}