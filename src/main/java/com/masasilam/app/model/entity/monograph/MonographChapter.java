package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographChapter {
    private Long id;
    private Long monographId;
    private Integer chapterOrder;
    private String title;
    private String narrativeSummary;
}