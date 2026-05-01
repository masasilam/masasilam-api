package com.naskah.demo.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateScheduleRequest {
    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;

    private Integer chapterStart;
    private Integer chapterEnd;
    private String chapterLabel;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String discussionPrompt;
}