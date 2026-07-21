package com.masasilam.app.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private Long sourceId;
    @Size(max = 255)
    private String sourceName;
    @NotBlank
    private String slug;
    @NotBlank
    private String category;
    @NotNull
    private LocalDate publishDate;
    @NotBlank
    private String title;
    @Size(max = 500)
    private String subtitle;
    @NotBlank
    private String htmlContent;
    private String author;
    private Integer pageNumber;
    private String importance;
    private String imageUrl;
    private Long parentArticleId;
    private Integer articleLevel;
}