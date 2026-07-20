package com.naskah.app.model.film;

import lombok.Data;

/**
 * Entitas utama film yang dipetakan ke tabel `film`.
 *
 * Kolom relasional (genre, person, company, dll) tidak disimpan di sini —
 * diambil secara terpisah via FilmMapper ketika membangun FilmDetail.
 */
@Data
public class Film {

    private Long   id;
    private String wikidataQid;
    private String slug;

    /** Judul dalam bahasa Indonesia (atau bahasa asli film) */
    private String judul;

    /** Judul dalam bahasa Inggris — dipakai sebagai dasar pembentukan slug */
    private String titleEng;

    /** Format: "-YYYY-MM-DD" (Wikidata time format, prefix + sudah di-strip) */
    private String tahunRilis;

    /** Jenis konten: film, film pendek, seri animasi, dll */
    private String jenis;

    private String deskripsi;
    private String catatan;
    private String durasi;
    private String negaraAsal;

    private String posterUrl;

    /** Beberapa URL gambar dipisahkan dengan "||" */
    private String imageUrls;

    /** URL video utama (untuk backward compat dengan data Wikidata lama) */
    private String videoUrl;
    private String trailerUrl;
    private String subtitleUrl;

    private String color;           // "color" atau "black-and-white"
    private String originalLanguage;
    private Integer copyrightStatusId;

    /** Budget dalam sen (cents) untuk menghindari floating-point error */
    private Long   budget;
    private String budgetDisplay;   // formatted string: "$1,000,000.00"

    private String followedBy;      // judul sekuel
    private String partOfSeries;    // nama seri

    /** Path file lokal (untuk fitur download lama, masih dipertahankan) */
    private String filePath;
}