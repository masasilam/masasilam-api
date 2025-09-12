package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadingNoteRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long bookId;

    private Integer page;
    private String position;

    @NotBlank
    private String noteContent;

    private String noteType;
    private String selectedText;
}
