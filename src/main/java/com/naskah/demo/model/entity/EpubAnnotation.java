package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Menyimpan highlight + catatan dari EPUB viewer.
 * Posisi diidentifikasi dengan CFI (Canonical Fragment Identifier),
 * bukan character offset — berbeda dari chapter HTML annotation.
 */
@Data
public class EpubAnnotation {

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

    /** Teks yang di-highlight */
    private String selectedText;

    /** Warna highlight, format hex, contoh: #FDE68A */
    private String color;

    /** Catatan opsional dari user */
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}