package com.naskah.app.service;

import com.naskah.app.model.dto.request.SubmitCorrectionRequest;
import com.naskah.app.model.dto.response.CorrectionResponse;
import com.naskah.app.model.dto.response.DataResponse;
import com.naskah.app.model.dto.response.DatatableResponse;

import java.util.List;

/**
 * Service untuk fitur laporan typo dan koreksi konten.
 *
 * Dua actor:
 *  1. User biasa  → submitCorrection()
 *  2. Admin       → getPendingCorrections(), approveCorrection(), rejectCorrection()
 */
public interface CorrectionService {

    // ── USER ────────────────────────────────────────────────

    /**
     * User submit laporan typo dari ChapterReaderPage.
     *
     * Validasi:
     *  - User harus login
     *  - originalText harus ditemukan di html_content chapter
     *  - Tidak boleh duplikat (same user + same chapter + same originalText)
     *
     * @param bookSlug      slug buku
     * @param chapterNumber nomor chapter
     * @param request       data koreksi dari frontend
     */
    DataResponse<CorrectionResponse> submitCorrection(
            String bookSlug,
            Integer chapterNumber,
            SubmitCorrectionRequest request);

    /**
     * Ambil posisi (startPosition) semua koreksi PENDING
     * untuk sebuah chapter.
     *
     * Dipakai frontend untuk render indikator ✎ di teks.
     * Endpoint: GET .../corrections/pending-positions
     */
    DataResponse<List<Integer>> getPendingPositions(
            String bookSlug,
            Integer chapterNumber);

    DatatableResponse<CorrectionResponse> getCorrections(String status, int page, int limit);

    /**
     * Admin menyetujui koreksi.
     *
     * Efek:
     *  1. UPDATE book_chapter.html_content (terapkan koreksi)
     *  2. UPDATE content_correction.status = APPROVED
     *  3. Evict cache chapter
     *  4. Trigger async rebuild epub (tidak menunggu selesai)
     *
     * @param correctionId ID koreksi yang disetujui
     */
    DataResponse<Void> approveCorrection(Long correctionId);

    /**
     * Admin menolak koreksi.
     *
     * @param correctionId ID koreksi yang ditolak
     * @param note         alasan penolakan (opsional)
     */
    DataResponse<Void> rejectCorrection(Long correctionId, String note);

    DataResponse<List<CorrectionResponse>> getMyPendingForBook(String slug);
}