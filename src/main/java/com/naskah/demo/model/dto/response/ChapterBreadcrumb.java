package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ChapterBreadcrumb {
    private Long chapterId;
    private String title;
    private String slug;
    private Integer chapterLevel;
}
