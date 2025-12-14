package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ChapterReadingResponse {
    private Long bookId;
    private String bookTitle;
    private Integer chapterNumber;
    private String chapterTitle;
    private String slug;
    private String content;
    private String htmlContent;
    private Integer wordCount;
    private Integer estimatedReadTime;

    // ✅ Hierarki info
    private Long parentChapterId;
    private Integer chapterLevel;
    private Long chapterId;

    // User annotations
    private List<BookmarkResponse> bookmarks;
    private List<HighlightResponse> highlights;
    private List<NoteResponse> notes;

    // Audio
    private ChapterAudioResponse audio;

    // Progress
    private Integer totalChapters;
    private Integer currentPosition;
    private Boolean isCompleted;

    // ✅ Navigation dengan hierarki
    private ChapterNavigationInfo previousChapter;
    private ChapterNavigationInfo nextChapter;
    private ChapterNavigationInfo parentChapter;
    private List<ChapterBreadcrumb> breadcrumbs;
}