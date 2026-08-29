package com.masasilam.app.model.dto.monograph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonographCriticalNoteInput {
    @NotNull
    private Integer number;
    @NotBlank
    private String noteText;
    private Long relatedArticleId;
}