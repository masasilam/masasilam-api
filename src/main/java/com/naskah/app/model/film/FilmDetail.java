package com.naskah.app.model.film;

import lombok.Data;

import java.util.List;

/**
 * Model DTO untuk detail film yang dikembalikan ke frontend.
 * <p>
 * Berisi semua informasi film termasuk relasi (pemain, sutradara, genre, dll),
 * video sources dari berbagai provider, serta skor ulasan dan rating konten.
 * <p>
 * Inner classes (BudgetData, BoxOfficeData, ReviewScore, ContentRating, VideoSource)
 * diimport dengan wildcard: import com.naskah.app.model.film.FilmDetail.*;
 */
@Data
public class FilmDetail {

    // ── Identitas ────────────────────────────────────────────────
    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;          // judul utama (ID / bahasa asli)
    private String judulSlug;      // judul EN — dipakai sebagai dasar slug
    private List<String> aliasIndonesia; // alias/alternatif nama di Indonesia

    // ── Metadata dasar ───────────────────────────────────────────
    private String tahunRilis;
    private String jenis;          // film, short film, animated series, dst
    private List<String> genre;
    private String deskripsi;
    private String catatan;
    private String durasi;         // contoh: "120 menit"
    private String negaraAsal;
    private String color;          // "color" | "black-and-white"
    private String originalLanguage;
    private Integer copyrightStatusId;

    // ── Kru & Pemain ─────────────────────────────────────────────
    private List<Person> sutradara;
    private List<Person> penulisSkenario;
    private List<Person> pemeran;
    private List<Person> produser;
    private List<Person> filmEditor;
    private List<Person> cinematographer;
    private List<Person> composer;
    private List<Person> narator;

    // ── Perusahaan ───────────────────────────────────────────────
    private List<Company> perusahaanProduksi;
    private List<Company> distributor;

    // ── Lokasi ───────────────────────────────────────────────────
    private List<String> narrativeLocation; // lokasi cerita (P840)
    private List<String> filmingLocation;   // lokasi syuting (P915)

    // ── Media ────────────────────────────────────────────────────
    private String posterUrl;
    private List<String> imageUrls;
    private String videoUrl;    // backward compat — video Wikimedia tunggal
    private String trailerUrl;  // backward compat — trailer Wikimedia tunggal
    private String subtitleUrl;

    /**
     * Daftar sumber video dari berbagai provider (YouTube, Archive.org, Vimeo, dll).
     * Menggantikan videoUrl/trailerUrl yang hanya mendukung satu sumber.
     * Diurutkan: priority DESC, is_trailer DESC.
     */
    private List<VideoSource> videoSources;

    // ── Finansial ────────────────────────────────────────────────
    private BudgetData budget;
    private List<BoxOfficeData> boxOffice;

    // ── Skor & Rating ────────────────────────────────────────────
    private List<ReviewScore> reviewScores;
    private List<ContentRating> contentRatings;

    // ── Relasi antar film ────────────────────────────────────────
    private String followedBy;    // judul film berikutnya dalam seri
    private String partOfSeries;  // nama seri

    // ================================================================
    // Inner Classes
    // ================================================================

    @Data
    public static class BudgetData {
        /**
         * Nilai dalam sen/cents agar tidak ada floating-point error
         */
        private Long amount;
        private String currency;
        /**
         * String terformat, misal "$1,500,000.00"
         */
        private String displayValue;
    }

    @Data
    public static class BoxOfficeData {
        private Long amount;
        private String currency;
        private String displayValue;
        /**
         * "worldwide", "United States", "Indonesia", dst
         */
        private String region;
    }

    @Data
    public static class ReviewScore {
        private String value;      // "94%", "8.3/10"
        private String source;     // "Rotten Tomatoes", "Metacritic"
        private String scoreType;  // "Tomatometer", "Metascore", "audience score"
        private Integer numReviews;
        private String reviewDate; // "-YYYY-MM-DD" format
    }

    @Data
    public static class ContentRating {
        private String system;              // "MPA", "BBFC", "FSK", "LSF", "EIRIN"
        private String value;               // "PG-13", "12A", "16", "SU", dst
        private String contentDescriptors;  // "Violence, Language" — joined by ", "
        private String startDate;
        private String distributionFormat;  // "theatrical release", "home video"
    }

    /**
     * Satu sumber video dari provider tertentu.
     * Sebuah film dapat memiliki beberapa VideoSource
     * (misal trailer dari YouTube + full movie dari Archive.org).
     */
    @Data
    public static class VideoSource {
        /**
         * URL asli yang dimasukkan admin
         */
        private String rawUrl;

        /**
         * YOUTUBE | ARCHIVE_ORG | VIMEO | DAILYMOTION | WIKIMEDIA | HLS | DASH | DIRECT_URL
         */
        private String providerType;

        /**
         * URL untuk iframe embed (null jika provider tidak mendukung)
         */
        private String embedUrl;

        /**
         * URL langsung ke file video (null jika provider tidak mengizinkan)
         */
        private String directUrl;

        /**
         * Thumbnail / poster frame video
         */
        private String thumbnailUrl;

        /**
         * Judul video dari provider
         */
        private String title;

        /**
         * Durasi dalam detik
         */
        private Integer durationSeconds;

        /**
         * true = trailer, false = full movie
         */
        private Boolean isTrailer;

        /**
         * Prioritas urutan tampil (makin besar = makin utama)
         */
        private Integer priority;
    }
}