package com.masasilam.app.model.entity.film;

import lombok.Data;

import java.util.List;

@Data
public class FilmDetail {
    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;
    private String judulSlug;
    private List<String> aliasIndonesia;
    private String tahunRilis;
    private String jenis;
    private List<String> genre;
    private String deskripsi;
    private String catatan;
    private String durasi;
    private String negaraAsal;
    private String color;
    private String originalLanguage;
    private Integer copyrightStatusId;
    private List<Person> sutradara;
    private List<Person> penulisSkenario;
    private List<Person> pemeran;
    private List<Person> produser;
    private List<Person> filmEditor;
    private List<Person> cinematographer;
    private List<Person> composer;
    private List<Person> narator;
    private List<Company> perusahaanProduksi;
    private List<Company> distributor;
    private List<String> narrativeLocation;
    private List<String> filmingLocation;
    private String posterUrl;
    private List<String> imageUrls;
    private String videoUrl;
    private String trailerUrl;
    private String subtitleUrl;
    private List<VideoSource> videoSources;
    private BudgetData budget;
    private List<BoxOfficeData> boxOffice;
    private List<ReviewScore> reviewScores;
    private List<ContentRating> contentRatings;
    private String followedBy;
    private String partOfSeries;

    @Data
    public static class BudgetData {
        private Long amount;
        private String currency;
        private String displayValue;
    }

    @Data
    public static class BoxOfficeData {
        private Long amount;
        private String currency;
        private String displayValue;
        private String region;
    }

    @Data
    public static class ReviewScore {
        private String value;
        private String source;
        private String scoreType;
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

    @Data
    public static class VideoSource {
        private String rawUrl;
        private String providerType;
        private String embedUrl;
        private String directUrl;
        private String thumbnailUrl;
        private String title;
        private Integer durationSeconds;
        private Boolean isTrailer;
        private Integer priority;
    }
}