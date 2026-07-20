package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Response untuk GET /api/dashboard/statistics
 *
 * Field baru (tidak breaking — frontend lama yang tidak pakai field ini
 * hanya akan mengabaikannya karena JSON deserialization toleran):
 *
 *   totalEpubChaptersRead    — jumlah bab dari sesi EPUB (terpisah dari chapter reader)
 *   estimatedReadingSpeedWpm — estimasi WPM dari total menit + bab, untuk EPUB
 *                              yang tidak punya data WPM akurat
 */
@Data
public class StatisticsResponse {

    /** Total buku unik yang dibaca dalam periode */
    private int totalBooksRead;

    /**
     * Total bab dari sesi chapter reader (non-EPUB).
     * Dihitung dari selisih startChapter → endChapter per sesi.
     */
    private int totalChaptersRead;

    /**
     * Total bab dari sesi EPUB.
     * Dihitung dari chaptersRead per sesi (= 1 per sesi, dengan chapterIndex
     * yang akurat setelah perbaikan EpubReaderPage).
     *
     * Frontend menampilkan breakdown: totalEpubChaptersRead + totalChaptersRead
     * atau bisa digabung sebagai total semua bab.
     */
    private int totalEpubChaptersRead;

    /** Total menit membaca dari semua sesi (EPUB + chapter reader) */
    private int totalReadingMinutes;

    /**
     * Kecepatan baca akurat dalam kata per menit.
     * Hanya terisi untuk sesi chapter reader yang mengirim word count.
     * 0 jika tidak ada data WPM akurat.
     */
    private double averageReadingSpeedWpm;

    /**
     * Estimasi kecepatan baca untuk sesi EPUB.
     * Dihitung dari: (totalBab × 3000 kata/bab) / totalMenit
     * Nilai diklem di rentang 80–600 wpm.
     * 0 jika tidak ada data sesi sama sekali.
     */
    private double estimatedReadingSpeedWpm;

    /** Tren waktu baca vs periode sebelumnya */
    private TrendData readingTimeTrend;

    /** Tren penyelesaian buku vs periode sebelumnya */
    private TrendData completionTrend;

    /**
     * Tren kecepatan baca.
     * Sebelumnya hardcoded "tidak tersedia untuk EPUB" — sekarang dihitung
     * dari estimatedReadingSpeedWpm antar dua periode jika WPM akurat tidak ada.
     */
    private TrendData speedTrend;

    /** Breakdown waktu baca per genre */
    private List<GenreBreakdownItem> genreBreakdown;

    /** Distribusi waktu baca per jam dalam sehari */
    private List<PeakReadingTimeItem> peakReadingTimes;
}