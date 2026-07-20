package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response untuk satu item koreksi.
 * Dipakai di:
 *  - GET  /api/dashboard/corrections  (admin list)
 *  - POST /api/books/.../corrections  (user submit, konfirmasi)
 */
@Data
public class CorrectionResponse {

    private Long   id;
    private Long   bookId;
    private String bookTitle;
    private Long   chapterId;
    private String chapterTitle;
    private Integer chapterNumber;

    // Informasi koreksi
    private String originalText;
    private String correctedText;
    private String contextBefore;
    private String contextAfter;
    private Integer startPosition;
    private Integer endPosition;
    private String userNote;

    private String epubCfi;
    private String sectionHref;

    // Untuk tampilan diff di admin panel:
    // "mereka saling [dipandanag] dengan penuh"
    //                      ↓
    // "mereka saling [dipandang] dengan penuh"
    private String diffPreview;

    // Workflow
    private String status; // PENDING / APPROVED / REJECTED

    // Pelapor
    private Long   submittedBy;
    private String submittedByUsername;

    // Admin reviewer (null jika masih PENDING)
    private Long   reviewedBy;
    private String reviewNote;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}