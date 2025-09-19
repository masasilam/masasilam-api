package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkRequest {
    @NotNull
    @Min(1)
    private Integer page;

    @NotNull
    private String position;

    @NotBlank
    private String title;

    private String description;
    private String color;
}