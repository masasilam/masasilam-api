package com.naskah.app.model.dto.request;

import com.naskah.app.model.dto.request.AddFilmRequest.CompanyInput;
import com.naskah.app.model.dto.request.AddFilmRequest.PersonInput;
import com.naskah.app.model.dto.request.AddFilmRequest.VideoSourceInput;
import lombok.Data;
import java.util.List;

@Data
public class UpdateFilmRequest {

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

    private List<String>           genre;
    private List<String>           aliasIndonesia;
    private List<String>           narrativeLocation;
    private List<String>           filmingLocation;
    private List<String>           imageUrls;

    private List<PersonInput>      sutradara;
    private List<PersonInput>      penulisSkenario;
    private List<PersonInput>      pemeran;
    private List<PersonInput>      produser;
    private List<PersonInput>      filmEditor;
    private List<PersonInput>      cinematographer;
    private List<PersonInput>      composer;
    private List<PersonInput>      narator;

    private List<CompanyInput>     perusahaanProduksi;
    private List<CompanyInput>     distributor;

    private List<VideoSourceInput> videoSources;
}