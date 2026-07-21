package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ContentEngagementAnalytics {
    private Integer totalAnnotations;
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private List<ChapterEngagement> topEngagedChapters;
    private Double averageAnnotationsPerChapter;
    private Double averageAnnotationsPerReader;
}