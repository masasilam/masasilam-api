package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ContentEngagementAnalytics {
    private Integer totalAnnotations;
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;

    // Top engaged chapters
    private List<ChapterEngagement> topEngagedChapters;

    // Annotation trends
    private Double averageAnnotationsPerChapter;
    private Double averageAnnotationsPerReader;
}