package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class FilmRatingStatsResponse {
    private Long filmId;
    private Double averageRating;
    private Long   totalRatings;

    // Jumlah per nilai bintang (dipakai oleh MyBatis resultMap)
    private Integer count50; // 5.0 ★
    private Integer count45; // 4.5 ★
    private Integer count40; // 4.0 ★
    private Integer count35; // 3.5 ★
    private Integer count30; // 3.0 ★
    private Integer count25; // 2.5 ★
    private Integer count20; // 2.0 ★
    private Integer count15; // 1.5 ★
    private Integer count10; // 1.0 ★
    private Integer count05; // 0.5 ★

    /**
     * Peta distribusi rating: "5.0" → jumlah, "4.5" → jumlah, dst.
     * Diisi oleh service setelah data dari DB diterima.
     * Berguna untuk bar chart di frontend.
     */
    private Map<String, Integer> distribution;
}