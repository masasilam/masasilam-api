package com.naskah.demo.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Request body untuk POST /api/books/{slug}/reading/epub-session
 *
 * Field progressIsAccurate (FIX GAP 1):
 *   true  → epubjs locations.generate() sudah selesai, progressPercent bisa dipercaya
 *   false → masih estimasi dari spine-index, backend perlu berhati-hati saat menyimpan
 *
 * Semua field lain tidak berubah dari versi sebelumnya.
 */
@Data
public class EpubSessionRequest {

    private String     sessionId;
    private Integer    durationSeconds;

    /** Persentase progres saat sesi berakhir (0–100). */
    private BigDecimal progressPercent;

    /**
     * FIX GAP 1: apakah progressPercent sudah dihitung dari epubjs locations
     * (akurat) atau masih estimasi berbasis spine-index (kasar)?
     *
     * Frontend mengirim true setelah locations.generate() selesai,
     * false jika user keluar sebelum generate() tuntas.
     *
     * Default: null → backend asumsikan true untuk backward-compatibility.
     */
    private Boolean    progressIsAccurate;

    private String     deviceType;
    private Integer    spineIndex;
    private Integer    totalSpineItems;
    private String     chapterLabel;
    private Integer    chapterIndex;
    private Integer    totalChapters;

    /** CFI posisi terakhir yang dibaca, disimpan ke reading_progress.current_position. */
    private String     lastCfi;
}