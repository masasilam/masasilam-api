package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AddFilmRequest {
    @NotBlank(message = "Judul wajib diisi")
    private String judul;
    private String judulSlug;
    private String tahunRilis;
    private String jenis;
    private String deskripsi;
    private String catatan;
    private String durasi;
    private String negaraAsal;
    private String originalLanguage;
    private String color;
    private Integer copyrightStatusId;
    private String posterUrl;
    private String trailerUrl;
    private String followedBy;
    private String partOfSeries;
    private List<String> genre;
    private List<String> aliasIndonesia;
    private List<String> narrativeLocation;
    private List<String> filmingLocation;
    private List<String> imageUrls;
    private List<PersonInput> sutradara;
    private List<PersonInput> penulisSkenario;
    private List<PersonInput> pemeran;
    private List<PersonInput> produser;
    private List<PersonInput> filmEditor;
    private List<PersonInput> cinematographer;
    private List<PersonInput> composer;
    private List<PersonInput> narator;
    private List<CompanyInput> perusahaanProduksi;
    private List<CompanyInput> distributor;
    private List<VideoSourceInput> videoSources;

    @Data
    public static class PersonInput {
        @NotBlank(message = "Nama person wajib diisi")
        private String name;
        private String photoUrl;
        private String description;
    }

    @Data
    public static class CompanyInput {
        @NotBlank(message = "Nama perusahaan wajib diisi")
        private String name;
        private String logoUrl;
        private String description;
    }

    @Data
    public static class VideoSourceInput {
        @NotBlank(message = "URL video wajib diisi")
        private String url;
        private Boolean isTrailer = false;
        private Integer priority = 0;
    }
}