package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChapterSearchResultResponse {
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private Integer chapterLevel;
    private String parentSlug;

    // Match details
    private List<SearchMatch> matches;
    private Integer matchCount;
    private Float relevanceScore;
}
