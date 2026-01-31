package com.naskah.demo.model.entity.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ============================================
// 1. NEWSPAPER SOURCE
// ============================================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperSource {
    private Long id;
    private String slug;
    private String name;
    private String nameOriginal;
    private String description;
    private String publisher;
    private String location;
    private Integer yearStart;
    private Integer yearEnd;
    private Integer totalArticles;
    private Long totalViews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}