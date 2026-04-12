package com.naskah.demo.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Request DTO untuk merekam sesi baca EPUB saat user meninggalkan halaman.
 *
 * Dikirim via dua jalur:
 *   1. fetch + keepalive (beforeunload — tab/browser ditutup)
 *   2. chapterService.recordEpubSession (React cleanup — navigasi internal)
 */
@Data
public class EpubSessionRequest {

    /** Client-generated UUID yang sama dengan yang dikirim saat startReading. */
    private String sessionId;

    /** Total durasi sesi dalam detik. */
    private Integer durationSeconds;

    /** Persentase progres baca saat sesi berakhir (0.0 – 100.0). */
    private BigDecimal progressPercent;

    /** Tipe perangkat: "mobile" atau "desktop". */
    private String deviceType;

    /** Spine index (0-based) saat sesi berakhir. Fallback jika chapterIndex null. */
    private Integer spineIndex;

    /** Total spine item dalam buku. Fallback jika totalChapters null. */
    private Integer totalSpineItems;

    // ── Chapter info dari TOC ─────────────────────────────────────────────────

    /** Label chapter aktif, mis. "Bab 3 - Pengkhianatan". Bisa null. */
    private String chapterLabel;

    /** Indeks chapter aktif dalam TOC top-level (0-based). Fallback ke spineIndex jika null. */
    private Integer chapterIndex;

    /** Total chapter top-level dalam TOC. Fallback ke totalSpineItems jika null. */
    private Integer totalChapters;

    /**
     * FIX: CFI posisi terakhir user membaca.
     * Disimpan ke reading_progress.current_position agar saat user
     * membuka ulang buku, reader bisa resume ke posisi yang tepat —
     * bukan hanya ke chapter, tapi ke paragraf/kata yang tepat.
     *
     * Contoh: "epubcfi(/6/10!/4/2/4:123)"
     */
    private String lastCfi;
}