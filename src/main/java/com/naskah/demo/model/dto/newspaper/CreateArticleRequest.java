package com.naskah.demo.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateArticleRequest {

    @NotNull
    private Long sourceId;

    @NotBlank
    private String slug;

    @NotBlank
    private String category;

    @NotNull
    private LocalDate publishDate;

    @NotBlank
    private String title;

    @NotBlank
    private String htmlContent;  // Akan diisi dari file upload

    private String author;
    private Integer pageNumber;
    private String importance;
    private String imageUrl;
    private Long parentArticleId;
    private Integer articleLevel;
}