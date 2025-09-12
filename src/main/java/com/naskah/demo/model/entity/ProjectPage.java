package com.naskah.demo.model.entity;

import com.naskah.demo.model.enums.OCRStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectPage {
    private Long id;
    private Long projectId;
    private Integer pageNumber;
    private String imageUrl;
    private String transcribedText;
    private String originalFilename;
    private OCRStatus ocrStatus;
    private Double ocrConfidence;
    private String ocrErrorMessage;
    private Long assignedUserId;
    private LocalDateTime completedAt;
    private String qualityNotes;
    private Integer revision;
    private Long reactionCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long translationCompletedBy;
    private Long editingCompletedBy;
    private Long illustrationCompletedBy;
    private Long proofreadingCompletedBy;
    private Long transcriptionCompletedBy;
}
