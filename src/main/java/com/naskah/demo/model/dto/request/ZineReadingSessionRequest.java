package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZineReadingSessionRequest {

    @NotNull(message = "startedAt tidak boleh kosong")
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Min(value = 0, message = "Durasi tidak boleh negatif")
    private Integer totalDurationSeconds = 0;

    /** Nomor chapter (1-based) yang sedang dibaca saat sesi dimulai */
    private Integer startChapter;

    /** Nomor chapter (1-based) yang sedang dibaca saat sesi berakhir */
    private Integer endChapter;

    /** Jumlah chapter yang dibaca dalam sesi ini (biasanya 1 per save) */
    private Integer chaptersRead = 1;

    /** Selalu "EPUB" untuk zine */
    private String sessionType = "EPUB";
}