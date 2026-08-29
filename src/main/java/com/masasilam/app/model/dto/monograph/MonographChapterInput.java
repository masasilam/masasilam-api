package com.masasilam.app.model.dto.monograph;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MonographChapterInput {
    @NotNull
    private Integer chapterOrder;
    @NotBlank
    private String title;
    private String narrativeSummary;
    @NotEmpty
    @Valid
    private List<MonographEntryInput> entries;
}