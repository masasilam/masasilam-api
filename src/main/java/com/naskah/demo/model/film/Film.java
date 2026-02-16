package com.naskah.demo.model.film;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Film {
    private Long id;
    private String wikidataQid;
    private String judul;
    private String slug;
    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private String durasi;
    private String negaraAsal;
    private String posterUrl;
    private String videoUrl;
    private String subtitleUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}