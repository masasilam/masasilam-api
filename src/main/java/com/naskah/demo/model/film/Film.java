package com.naskah.demo.model.film;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Film entity - mapped to 'films' table.
 * Tambahan field: titleEng (untuk menyimpan judul bahasa Inggris / slug source)
 *
 * SQL migration yang diperlukan:
 *   ALTER TABLE films ADD COLUMN title_eng VARCHAR(500);
 */
@Data
public class Film {

    private Long id;
    private String wikidataQid;
    private String slug;

    /** Judul dalam bahasa asli / Indonesia */
    private String judul;

    /**
     * Judul bahasa Inggris — digunakan sebagai slug source.
     * Diisi dari: P1476+P2441, label EN, atau hasil translate MyMemory.
     * Jika judul sudah latin, nilai ini sama dengan judul.
     */
    private String titleEng;

    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private String durasi;
    private String negaraAsal;

    private String posterUrl;
    private String imageUrls;
    private String videoUrl;
    private String trailerUrl;
    private String subtitleUrl;

    private String color;
    private String originalLanguage;

    private Long budget;
    private String budgetDisplay;

    private String followedBy;
    private String partOfSeries;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}