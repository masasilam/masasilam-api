package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChapterReadingResponse {
    private Long bookId;
    private String bookTitle;
    private String bookSubtitle;
    private Integer chapterNumber;
    private String chapterTitle;
    private String slug;
    private String content;
    private String htmlContent;
    private Integer wordCount;
    private Integer estimatedReadTime;
    private Long parentChapterId;
    private Integer chapterLevel;
    private Long chapterId;
    private List<BookmarkResponse> bookmarks;
    private List<HighlightResponse> highlights;
    private List<NoteResponse> notes;
    private Integer totalChapters;
    private Integer currentPosition;
    private Boolean isCompleted;
    private ChapterNavigationInfo previousChapter;
    private ChapterNavigationInfo nextChapter;
    private ChapterNavigationInfo parentChapter;
    private List<ChapterBreadcrumb> breadcrumbs;
}