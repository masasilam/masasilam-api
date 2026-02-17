package com.naskah.demo.model.film;

import lombok.Data;
import java.util.List;

/**
 * FilmDetail - Complete film information including all relationships
 */
@Data
public class FilmDetail {
    // ==================== BASIC INFO ====================
    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;
    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private List<String> genre;

    // ==================== PEOPLE - CORE ROLES ====================
    private List<Person> sutradara;         // Directors (P57)
    private List<Person> penulisSkenario;   // Screenwriters (P58)
    private List<Person> pemeran;           // Cast members (P161)
    private List<Person> produser;          // Producers (P162)

    // ==================== PEOPLE - ADDITIONAL CREW ====================
    private List<Person> filmEditor;        // Film editors (P1040)
    private List<Person> cinematographer;   // Cinematographers/DOP (P344)
    private List<Person> composer;          // Music composers (P86)

    // ==================== COMPANIES ====================
    private List<Company> perusahaanProduksi;  // Production companies (P272)
    private List<Company> distributor;          // Distributors (P750)

    // ==================== LOCATIONS ====================
    private String negaraAsal;              // Country of origin (P495)
    private List<String> narrativeLocation; // Story setting (P840)
    private List<String> filmingLocation;   // Filming locations (P915)
    private String durasi;                  // Duration (P2047)

    // ==================== TECHNICAL INFO ====================
    private String color;                   // black-and-white / color (P462)
    private String originalLanguage;        // Original language (P364)
    private String posterUrl;               // Poster image (P18)
    private List<String> imageUrls;
    private String videoUrl;                // Full video file (P10 with role Q89347362)
    private String trailerUrl;              // Trailer video (P10 with role Q622550)
    private String subtitleUrl;             // Subtitle file

    // ==================== FINANCIAL DATA ====================
    private BudgetData budget;              // Budget/capital cost (P2130)
    private List<BoxOfficeData> boxOffice;  // Box office by region (P2142)

    // ==================== RATINGS & REVIEWS ====================
    private List<ReviewScore> reviewScores;     // Review scores (P444)
    private List<ContentRating> contentRatings; // Content ratings (P1657, P2758, etc.)

    // ==================== RELATIONS ====================
    private String followedBy;              // Sequel (P156)
    private String partOfSeries;            // Part of series (P179)

    // ==================== ALIASES ====================
    private List<String> aliasIndonesia;    // Indonesian aliases

    // ==================== NESTED CLASSES ====================

    /**
     * Box Office Data - Revenue by region
     */
    @Data
    public static class BoxOfficeData {
        private String region;          // 'worldwide', 'United States', etc.
        private Long amount;            // Amount in cents
        private String currency;        // 'USD', 'EUR', etc.
        private String displayValue;    // Formatted: "$30,087,064"
    }

    /**
     * Budget Data - Production cost
     */
    @Data
    public static class BudgetData {
        private Long amount;            // Amount in cents
        private String currency;        // 'USD', 'EUR', etc.
        private String displayValue;    // Formatted: "$114,000"
    }

    /**
     * Review Score - Critics' scores
     */
    @Data
    public static class ReviewScore {
        private String source;          // 'Rotten Tomatoes', 'Metacritic', etc.
        private String scoreType;       // 'Tomatometer', 'Average Rating', 'Metascore'
        private String value;           // '95%', '8.8/10', '89/100'
        private Integer numReviews;     // Number of critic reviews
        private String reviewDate;      // When the score was recorded
    }

    /**
     * Content Rating - Age/content ratings
     */
    @Data
    public static class ContentRating {
        private String system;              // 'MPA', 'BBFC', 'FSK', 'EIRIN', etc.
        private String value;               // 'R', '18', '16', 'G', etc.
        private String contentDescriptors;  // 'Violence', 'Horror', etc.
        private String startDate;           // When rating was issued
        private String distributionFormat;  // 'theatrical', 'home video', 'streaming'
    }
}