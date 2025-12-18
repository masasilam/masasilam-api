package com.naskah.demo.model.entity;

import lombok.Data;

@Data
public class ChapterSearchResult {
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private String snippet; // Text snippet with match highlighted
    private Float relevanceScore;
    private Integer matchCount;
    private Integer wordPosition; // Position of match in content
}
