package com.masasilam.app.model.film;

import lombok.Data;

@Data
public class Film {
    private Long id;
    private String wikidataQid;
    private String slug;
    private String judul;
    private String titleEng;
    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private String catatan;
    private String durasi;
    private String negaraAsal;
    private String posterUrl;
    private String imageUrls;
    private String videoUrl;
    private String trailerUrl;
    private String subtitleUrl;
    private String color;
    private String originalLanguage;
    private Integer copyrightStatusId;
    private Long budget;
    private String budgetDisplay;
    private String followedBy;
    private String partOfSeries;
    private String filePath;
}