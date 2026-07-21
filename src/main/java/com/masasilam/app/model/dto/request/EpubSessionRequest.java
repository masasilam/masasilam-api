package com.masasilam.app.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EpubSessionRequest {
    private String sessionId;
    private Integer durationSeconds;
    private BigDecimal progressPercent;
    private Boolean progressIsAccurate;
    private String deviceType;
    private Integer spineIndex;
    private Integer totalSpineItems;
    private String chapterLabel;
    private Integer chapterIndex;
    private Integer totalChapters;
    private String lastCfi;
}