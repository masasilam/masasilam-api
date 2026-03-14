package com.naskah.demo.model.dto.newspaper;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateArticleRequest {

    @NotBlank
    private String title;

    @Size(max = 500)
    private String subtitle;

    // Tidak wajib @NotBlank — digenerate otomatis dari htmlContent di service
    private String content;

    private String htmlContent;

    // ✅ FIX: tambah slug agar URL artikel bisa diupdate saat edit
    @Size(max = 500)
    private String slug;

    private String category;
    private LocalDate publishDate;

    private Long sourceId;

    @Size(max = 255)
    private String sourceName;

    private String author;
    private Integer pageNumber;
    private String importance;
    private String imageUrl;
}