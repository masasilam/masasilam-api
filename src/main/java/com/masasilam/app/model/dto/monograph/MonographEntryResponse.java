package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MonographEntryResponse {
    private Long articleId;
    private String articleSlug;
    private String sourceSlug;
    private String sourceName;
    private String title;
    private String subtitle;
    private String leadSnippet;
    private Integer pageNumber;
    private String columnInfo;
    private LocalDate publishDate;
    private Integer wordCount;
    private String role;
    private String textStatus;
    private String positionLabel;
    private String editorialNote;
    private Integer entryOrder;
    private Long chapterId;
    private Long parentArticleId;
    private Integer articleLevel;
    private boolean continuation;
}