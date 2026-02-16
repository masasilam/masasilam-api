package com.naskah.demo.model.film;

import lombok.Data;
import java.util.List;

@Data
public class FilmDetail {
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
    private List<String> genre;
    private List<Person> sutradara;
    private List<Person> penulisSkenario;
    private List<Person> pemeran;
    private List<Person> produser;
    private List<Company> perusahaanProduksi;
    private List<String> aliasIndonesia;
}
