package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;

/**
 * Service interface untuk operasi EPUB reader:
 *   - Annotations (highlight + catatan)
 *   - Bookmarks
 *   - Reading session tracking (start, end, record)
 */
public interface EpubAnnotationService {

    // ── Bundle ────────────────────────────────────────────────────────────────

    /** Ambil semua annotations + bookmarks milik user untuk satu buku. */
    DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug);

    // ── Annotations ───────────────────────────────────────────────────────────

    /** Tambah annotation baru (highlight / highlight + catatan). */
    DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request);

    /** Hapus annotation berdasarkan ID. Hanya bisa hapus milik sendiri. */
    DataResponse<Void> deleteAnnotation(String bookSlug, Long annotationId);

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    /** Tambah bookmark baru pada posisi CFI tertentu. */
    DataResponse<EpubBookmarkResponse> addBookmark(String bookSlug, EpubBookmarkRequest request);

    /** Hapus bookmark berdasarkan ID. Hanya bisa hapus milik sendiri. */
    DataResponse<Void> deleteBookmark(String bookSlug, Long bookmarkId);

    // ── Reading Session ───────────────────────────────────────────────────────

    /**
     * Dipanggil saat EpubReaderPage pertama kali mount.
     *
     * Menggunakan EpubStartReadingRequest (bukan StartReadingRequest) karena
     * EPUB tidak mengenal chapterNumber dari DB — chapter diidentifikasi via
     * TOC dari dalam file .epub (chapterLabel, chapterIndex, totalChapters).
     */
    DataResponse<EpubStartReadingResponse> startReading(String slug, EpubStartReadingRequest request);

    /**
     * No-op untuk EPUB. Data sesi direkam via recordEpubSession.
     * Tersedia agar controller tetap konsisten.
     */
    DataResponse<Void> endReading(String slug, EndReadingRequest request);

    /**
     * Rekam satu sesi baca EPUB secara lengkap.
     * Dipanggil saat user meninggalkan halaman (beforeunload / React cleanup).
     * Menyimpan reading_session dan upsert reading_progress.
     */
    DataResponse<Void> recordEpubSession(String slug, EpubSessionRequest request);
}