package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CorrectionResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long chapterId;
    private String chapterTitle;
    private Integer chapterNumber;
    private String originalText;
    private String correctedText;
    private String contextBefore;
    private String contextAfter;
    private Integer startPosition;
    private Integer endPosition;
    private String userNote;
    private String epubCfi;
    private String sectionHref;
    private String diffPreview;
    private String status;
    private Long submittedBy;
    private String submittedByUsername;
    private Long reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}