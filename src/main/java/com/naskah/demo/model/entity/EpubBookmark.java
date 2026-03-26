package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Menyimpan posisi bookmark dari EPUB viewer.
 * Menggunakan CFI sebagai penanda posisi.
 */
@Data
public class EpubBookmark {

    private Long id;

    /** FK ke users */
    private Long userId;

    /** FK ke books */
    private Long bookId;

    /**
     * CFI dari epub.js, contoh:
     * epubcfi(/6/4[chap01]!/4/2/1:0)
     */
    private String cfi;

    /** Label singkat yang ditampilkan di sidebar — biasanya "Halaman X" atau "Posisi Y%" */
    private String label;

    private LocalDateTime createdAt;
}