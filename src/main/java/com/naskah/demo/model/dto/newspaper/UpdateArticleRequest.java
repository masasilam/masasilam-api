package com.naskah.demo.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArticleRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String htmlContent;

    private String author;

    private Integer pageNumber;

    private String importance;

    private String imageUrl;
}