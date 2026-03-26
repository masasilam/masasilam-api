package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity untuk laporan typo dari user.
 *
 * Alur:
 *  1. User select teks salah di ChapterReaderPage
 *  2. Klik "Laporkan Typo" → isi koreksi
 *  3. POST /api/books/{slug}/chapters/{num}/corrections
 *  4. Insert ContentCorrection dengan status PENDING
 *  5. Admin review di dashboard
 *  6. Approve → UPDATE book_chapter.html_content + trigger epub rebuild
 */
@Data
public class ContentCorrection {

    private Long id;

    // Referensi ke buku dan chapter
    private Long bookId;
    private Long chapterId;

    // User yang melaporkan
    private Long submittedBy;

    // Teks yang dilaporkan salah (persis seperti di HTML)
    private String originalText;

    // Usulan perbaikan
    private String correctedText;

    // Konteks sekitar teks untuk replace yang presisi
    // ~50 char sebelum dan sesudah originalText
    // Digunakan di CorrectionServiceImpl.applyCorrection()
    private String contextBefore;
    private String contextAfter;

    // Posisi di dalam html_content (character offset)
    private Integer startPosition;
    private Integer endPosition;

    // Catatan tambahan dari user (opsional)
    private String userNote;

    // Workflow: PENDING → APPROVED atau REJECTED
    private String status;

    // Diisi saat admin review
    private Long reviewedBy;
    private String reviewNote;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // Field tambahan untuk JOIN query (tidak di-persist)
    // Dipakai di CorrectionResponse mapping
    private String chapterTitle;
    private String bookTitle;
    private String submittedByUsername;
    private Integer chapterNumber;
}