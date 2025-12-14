package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterNavigationInfo {
    private Integer chapterNumber;
    private String title;
    private Integer chapterLevel;
    private String slug;
    private String parentSlug;
}