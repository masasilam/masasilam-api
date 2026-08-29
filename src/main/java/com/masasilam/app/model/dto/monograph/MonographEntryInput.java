package com.masasilam.app.model.dto.monograph;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonographEntryInput {
    @NotNull
    private Long articleId;
    private String textStatus;
    private Integer order;
    private String positionLabel;
    private String columnInfo;
    private String editorialNote;
}