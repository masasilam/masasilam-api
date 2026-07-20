package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response body untuk POST /api/books/{slug}/reading/start
 *
 * Field lastReadAt (FIX GAP 4):
 *   Timestamp kapan reading_progress terakhir diupdate di server.
 *   Frontend membandingkan ini dengan timestamp localStorage untuk menentukan
 *   mana yang lebih baru dan harus dipakai sebagai posisi resume.
 *
 *   Jika lastReadAt > localStorage.progressAt → pakai lastCfi dari server.
 *   Jika lastReadAt <= localStorage.progressAt → pakai CFI dari localStorage.
 *
 * Semua field lain tidak berubah dari versi sebelumnya.
 */
@Data
public class EpubStartReadingResponse {

    /** true jika ini adalah pertama kali user membaca buku ini. */
    private boolean firstTime;

    /** CFI posisi terakhir dari reading_progress. Null jika firstTime = true. */
    private String lastCfi;

    /** Persentase progres terakhir (0–100). Null jika firstTime = true. */
    private Double lastProgress;

    /**
     * Index chapter terakhir (0-based, dari TOC).
     * Null jika firstTime = true atau belum pernah ada chapter yang tercatat.
     */
    private Integer lastChapterIndex;

    /** Total chapter buku dari TOC. Null jika belum pernah tercatat. */
    private Integer totalChapters;

    /**
     * FIX GAP 4: timestamp kapan reading_progress.last_read_at terakhir diupdate
     * di server. Dipakai frontend untuk membandingkan dengan localStorage.progressAt
     * dan menentukan sumber resume yang lebih baru (server vs lokal).
     *
     * Null jika firstTime = true.
     */
    private LocalDateTime lastReadAt;
}