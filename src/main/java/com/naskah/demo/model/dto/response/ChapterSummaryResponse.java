package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ChapterSummaryResponse {
    private Long id;
    private Integer chapterNumber;
    private Long parentChapterId;
    private Integer chapterLevel;
    private String slug;
    private String title;
    private Integer wordCount;
    private Integer estimatedReadTime;
    private Boolean hasAudio;
    private Boolean isCompleted;
    private List<ChapterSummaryResponse> subChapters;
}