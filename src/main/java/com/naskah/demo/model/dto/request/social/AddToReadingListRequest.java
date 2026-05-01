package com.naskah.demo.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToReadingListRequest {
    @NotBlank
    private String entityType; // BOOK, ZINE, FILM, NEWSPAPER

    @NotNull
    private Long entityId;

    private String note;
}