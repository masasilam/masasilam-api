package com.naskah.demo.model.film;

import lombok.Data;
import java.util.List;

@Data
public class FilmDetail {

    // ==================== IDENTITAS ====================

    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;
    private String judulSlug;

    // ==================== INFO DASAR ====================

    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private String durasi;
    private String negaraAsal;

    // ==================== MEDIA ====================

    private String posterUrl;
    private List<String> imageUrls;
    private String videoUrl;
    private String trailerUrl;
    private String subtitleUrl;

    // ==================== TEKNIS ====================

    private String color;
    private String originalLanguage;
    private BudgetData budget;

    // ==================== RELASI ORANG ====================

    private List<Person> sutradara;
    private List<Person> penulisSkenario;
    private List<Person> pemeran;
    private List<Person> produser;
    private List<Person> filmEditor;
    private List<Person> cinematographer;
    private List<Person> composer;

    // ==================== RELASI PERUSAHAAN ====================

    private List<Company> perusahaanProduksi;
    private List<Company> distributor;

    // ==================== LOKASI ====================

    private List<String> narrativeLocation;
    private List<String> filmingLocation;

    // ==================== GENRE & KLASIFIKASI ====================

    private List<String> genre;
    private List<ContentRating> contentRatings;

    // ==================== BOX OFFICE & REVIEW ====================

    private List<BoxOfficeData> boxOffice;
    private List<ReviewScore> reviewScores;

    // ==================== SERI & ALIAS ====================

    private String followedBy;
    private String partOfSeries;
    private List<String> aliasIndonesia;

    // ==================== INNER CLASSES ====================

    @Data
    public static class BudgetData {
        private Long amount;
        private String currency;
        private String displayValue;
    }

    @Data
    public static class BoxOfficeData {
        private String region;
        private Long amount;
        private String currency;
        private String displayValue;
    }

    @Data
    public static class ReviewScore {
        private String source;
        private String scoreType;
        private String value;
        private Integer numReviews;
        private String reviewDate;
    }

    @Data
    public static class ContentRating {
        private String system;
        private String value;
        private String contentDescriptors;
        private String startDate;
        private String distributionFormat;
    }
}