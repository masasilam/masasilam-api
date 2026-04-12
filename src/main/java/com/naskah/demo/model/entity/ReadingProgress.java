package com.naskah.demo.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReadingProgress {

    private Long          id;
    private Long          userId;
    private Long          bookId;

    /**
     * Untuk EPUB  : spine index 1-based (chapter ke berapa dalam TOC)
     * Untuk Chapter: nomor chapter saat ini
     */
    private Integer       currentPage;

    /**
     * Untuk EPUB  : totalChapters dari TOC (atau totalSpineItems sebagai fallback)
     * Untuk Chapter: total halaman/chapter buku
     */
    private Integer       totalPages;

    /**
     * Untuk EPUB  : CFI string terakhir (mis. "epubcfi(/6/10!/4/2/4:123)")
     *               Dipakai untuk resume ke posisi tepat saat user membuka ulang buku.
     * Untuk Chapter: bisa dipakai untuk character offset, opsional.
     */
    private String        currentPosition;

    private BigDecimal    percentageCompleted;
    private Integer       readingTimeMinutes;

    /**
     * Status: "reading" | "completed" | "not_started"
     */
    private String        status;

    private Boolean       isFavorite;
    private String        notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastReadAt;
}