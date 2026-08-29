package com.masasilam.app.model.entity.monograph;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Monograph {
    private Long id;
    private String slug;
    private String title;
    private String subtitle;
    private Long newspaperSourceId;
    private LocalDate editionDate;
    private LocalDate editionDateTo;
    private String editorialNote;
    private String colophon;
    private String centralQuestion;
    private String methodNote;
    private String coverImageUrl;
    private String citationFormatTemplate;
    private String status;
    private Integer viewCount;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}