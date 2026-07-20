 // ============================================================
// FILE 1: model/dto/request/SubmitCorrectionRequest.java
// ============================================================
package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body untuk user submit laporan typo.
 *
 * POST /api/books/{slug}/chapters/{chapterNumber}/corrections
 *
 * Frontend mengisi ini dari TextSelectionPopup:
 *  - originalText  = teks yang di-select user (yang salah)
 *  - correctedText = input user di modal koreksi
 *  - contextBefore = 50 char sebelum selection (dari JS)
 *  - contextAfter  = 50 char sesudah selection (dari JS)
 *  - startPosition = selectionRange.startOffset
 *  - endPosition   = selectionRange.endOffset
 */
@Data
public class SubmitCorrectionRequest {

    @NotBlank(message = "Teks asli tidak boleh kosong")
    @Size(max = 500, message = "Teks asli maksimal 500 karakter")
    private String originalText;

    @NotBlank(message = "Teks koreksi tidak boleh kosong")
    @Size(max = 500, message = "Teks koreksi maksimal 500 karakter")
    private String correctedText;

    // Konteks untuk presisi replace — tidak wajib tapi sangat disarankan
    @Size(max = 100)
    private String contextBefore;

    @Size(max = 100)
    private String contextAfter;

    private String epubCfi;
    private String sectionHref;

    // Posisi karakter di html_content
    private Integer startPosition;
    private Integer endPosition;

    // Catatan tambahan dari user (opsional)
    @Size(max = 500, message = "Catatan maksimal 500 karakter")
    private String userNote;
}
