package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.util.List;

@Data
public class MonographChapterResponse {
    private Long id;
    private Integer chapterOrder;
    private String title;
    private String narrativeSummary;
    private List<MonographEntryResponse> entries;
}