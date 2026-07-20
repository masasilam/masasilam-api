package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response DTO untuk satu buku yang sedang dibaca — ditampilkan di
 * dashboard "Sedang Dibaca" dan perpustakaan.
 *
 * FIX yang ditambahkan:
 *   - isEpub     : flag eksplisit apakah buku ini dibaca via EpubReaderPage.
 *                  Dipakai frontend untuk menentukan URL tujuan saat buku diklik
 *                  (/buku/{slug}/baca vs /buku/{slug}) dan label posisi baca.
 *   - lastCfi    : CFI posisi terakhir dari reading_progress.current_position.
 *                  Dikirim ke EpubReaderPage via router state agar reader
 *                  bisa resume ke posisi tepat, bukan hanya ke awal chapter.
 *   - chapterLabel: label chapter aktif dari TOC (mis. "Bab 3 - Pengkhianatan").
 *                  Ditampilkan di UI sebagai subtitle yang informatif.
 */
@Data
public class BookInProgressResponse {

    private Long          bookId;
    private String        bookSlug;
    private String        bookTitle;
    private String        authorName;
    private String        coverImageUrl;
    private double        progressPercentage;
    private LocalDateTime lastReadAt;

    /**
     * Untuk EPUB  : spine index 1-based (chapter ke berapa dalam TOC).
     * Untuk Chapter: nomor chapter saat ini.
     *
     * Jangan gunakan ini untuk menentukan apakah buku adalah EPUB —
     * gunakan flag isEpub di bawah.
     */
    private Integer currentChapter;

    /**
     * Untuk EPUB  : totalChapters dari TOC.
     * Untuk Chapter: total chapter/halaman buku.
     */
    private Integer totalChapters;

    /**
     * FIX: Label chapter dari TOC, mis. "Bab 3 - Pengkhianatan".
     * Null jika buku non-EPUB atau TOC tidak tersedia.
     * Ditampilkan di dashboard sebagai subtitle informatif.
     */
    private String chapterLabel;

    /**
     * FIX: true jika sesi terbaru buku ini bertipe EPUB.
     * Dipakai frontend untuk:
     *   - Menentukan URL tujuan: /buku/{slug}/baca (EPUB) vs /buku/{slug} (Chapter)
     *   - Menentukan label posisi: "34% selesai" vs "Bab 3/10 · 34%"
     */
    private boolean isEpub;

    /**
     * FIX: CFI string posisi terakhir dari reading_progress.current_position.
     * Dikirim sebagai router state ke EpubReaderPage sehingga reader bisa
     * langsung display ke posisi yang tepat tanpa user harus scroll manual.
     *
     * Null untuk buku non-EPUB atau jika belum ada progress tersimpan.
     * Contoh: "epubcfi(/6/10!/4/2/4:123)"
     */
    private String lastCfi;
}