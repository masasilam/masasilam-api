package com.masasilam.app.model.dto.monograph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMonographRequest {
    private String slug;
    @NotBlank
    private String title;
    private String subtitle;
    private Long sourceId;
    private String sourceName;
    @NotNull
    private LocalDate editionDate;
    private LocalDate editionDateTo;
    private String editorialNote;
    private String colophon;
    private String centralQuestion;
    private String methodNote;
    private String coverImageUrl;
    private String citationFormatTemplate;
}