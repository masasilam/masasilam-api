package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentCorrection {
    private Long id;
    private Long bookId;
    private Long chapterId;
    private Long submittedBy;
    private String originalText;
    private String correctedText;
    private String contextBefore;
    private String contextAfter;
    private Integer startPosition;
    private Integer endPosition;
    private String userNote;
    private String status;
    private Long reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String chapterTitle;
    private String bookTitle;
    private String submittedByUsername;
    private Integer chapterNumber;
    private String epubCfi;
    private String sectionHref;
}