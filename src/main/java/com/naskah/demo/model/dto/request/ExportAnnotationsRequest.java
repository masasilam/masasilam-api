package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportAnnotationsRequest {
    @NotBlank(message = "Export type is required")
    @Pattern(regexp = "PDF|DOCX|JSON|HTML|MD", message = "Invalid export type")
    private String exportType;

    // What to include
    private Boolean includeBookmarks = true;
    private Boolean includeHighlights = true;
    private Boolean includeNotes = true;

    // Filtering
    private Integer chapterFrom;
    private Integer chapterTo;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    // Options
    private Boolean includeChapterTitles = true;
    private Boolean includeTimestamps = true;
    private Boolean groupByChapter = true;
}