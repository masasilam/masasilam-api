package com.naskah.demo.model.film;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Film entity - Main film/movie data
 */
@Data
public class Film {
    // ==================== PRIMARY FIELDS ====================
    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;
    private String tahunRilis;
    private String jenis;
    private String deskripsi;

    // ==================== LOCATION & DURATION ====================
    private String negaraAsal;
    private String durasi;

    // ==================== MEDIA ====================
    private String posterUrl;
    private String videoUrl;        // Full video
    private String trailerUrl;      // Trailer video
    private String subtitleUrl;

    // ==================== TECHNICAL INFO ====================
    private String color;                // black-and-white / color
    private String originalLanguage;     // English, Indonesian, etc.

    // ==================== FINANCIAL INFO ====================
    private Long budget;                 // Budget in cents (multiply by 100)
    private String budgetDisplay;        // Formatted: "$114,000"

    // ==================== RELATIONS ====================
    private String followedBy;           // Sequel title
    private String partOfSeries;         // Series name

    // ==================== TIMESTAMPS ====================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}