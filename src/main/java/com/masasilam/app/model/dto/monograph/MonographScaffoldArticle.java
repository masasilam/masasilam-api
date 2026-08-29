package com.masasilam.app.model.dto.monograph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographScaffoldArticle {
    private Long articleId;
    private String title;
    private String subtitle;
    private String slug;
    private Integer pageNumber;
    private Integer wordCount;
    private LocalDate publishDate;
    private Long parentArticleId;
    private Integer articleLevel;
    private boolean isContinuation;
    private String suggestedRole;
}