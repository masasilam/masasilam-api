package com.naskah.app.model.dto.request;

import lombok.Data;

/**
 * Request DTO khusus untuk EPUB reader saat pertama kali mount.
 *
 * Mengapa dipisah dari StartReadingRequest:
 *   - StartReadingRequest memiliki @NotNull pada chapterNumber karena
 *     chapter reader memang wajib mengirim nomor chapter.
 *   - EPUB reader tidak mengenal chapterNumber — posisi direpresentasikan
 *     sebagai CFI dan spine index. Memaksa keduanya berbagi satu DTO
 *     akan menghilangkan validasi yang dibutuhkan chapter reader.
 *
 * Field chapter (chapterLabel, chapterIndex, totalChapters) bersumber
 * dari TOC (Table of Contents) yang ada di dalam file .epub itu sendiri,
 * bukan dari database chapter.
 */
@Data
public class EpubStartReadingRequest {

    /** Client-generated UUID untuk sesi ini. */
    private String sessionId;

    /** Tipe perangkat: "mobile" atau "desktop". */
    private String deviceType;

    /** Sumber: selalu "epub" untuk request ini. */
    private String source;

    /**
     * Label chapter yang sedang dibaca saat reader pertama kali mount.
     * Contoh: "Bab 1 - Kedatangan", "Prolog", "Chapter 3".
     * Bisa kosong jika TOC belum siap saat startReading dipanggil.
     */
    private String chapterLabel;

    /**
     * Indeks chapter saat ini dalam daftar TOC top-level (0-based).
     * Contoh: 0 untuk chapter pertama, 1 untuk kedua, dst.
     */
    private Integer chapterIndex;

    /**
     * Total chapter top-level (depth = 0) dalam TOC buku.
     * Digunakan untuk menghitung progress per chapter di backend.
     */
    private Integer totalChapters;
}
