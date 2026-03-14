package com.naskah.demo.model.dto.newspaper;

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

    // sourceId: pakai jika sumber sudah terdaftar
    private Long sourceId;

    // sourceName: pakai jika sumber belum terdaftar, akan dibuat otomatis
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
    private String subtitle;           // ← BARU: sub judul, opsional

    @NotBlank
    private String htmlContent;

    private String author;
    private Integer pageNumber;
    private String importance;
    private String imageUrl;
    private Long parentArticleId;
    private Integer articleLevel;
}