package com.naskah.demo.controller.book;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.EpubAnnotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint khusus untuk EPUB viewer.
 *
 * Mengapa dipisah dari BookChapterController:
 *  - Posisi direpresentasikan sebagai CFI, bukan character offset
 *  - Tidak ada relasi dengan chapterNumber — EPUB bekerja lintas spine item
 *  - Chapter tracking menggunakan TOC dari dalam file .epub, bukan dari DB chapter
 *
 * Base path: /api/books/{slug}/epub-annotations
 *             /api/books/{slug}/epub-bookmarks
 *             /api/books/{slug}/reading/...
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/books/{slug}")
@RequiredArgsConstructor
public class EpubAnnotationController {

    private final EpubAnnotationService epubAnnotationService;

    // ════════════════════════════════════════════════════════════
    // BUNDLE — satu call untuk load semua data saat mount
    // ════════════════════════════════════════════════════════════

    /**
     * GET /api/books/{slug}/epub-annotations
     *
     * Mengembalikan semua annotations + bookmarks milik user untuk buku ini.
     * Dipakai frontend sekali saat EpubReaderPage pertama kali mount.
     *
     * Auth: wajib login
     */
    @GetMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationsBundleResponse>> getAll(
            @PathVariable String slug) {

        DataResponse<EpubAnnotationsBundleResponse> response = epubAnnotationService.getAll(slug);
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════════
    // ANNOTATIONS (highlight + catatan)
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/books/{slug}/epub-annotations
     *
     * Body: { cfi, selectedText, color?, note? }
     * Auth: wajib login
     */
    @PostMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationResponse>> addAnnotation(
            @PathVariable String slug,
            @Valid @RequestBody EpubAnnotationRequest request) {

        DataResponse<EpubAnnotationResponse> response = epubAnnotationService.addAnnotation(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/books/{slug}/epub-annotations/{annotationId}
     *
     * Auth: wajib login, hanya bisa hapus milik sendiri
     */
    @DeleteMapping("/epub-annotations/{annotationId}")
    public ResponseEntity<DataResponse<Void>> deleteAnnotation(
            @PathVariable String slug,
            @PathVariable Long annotationId) {

        DataResponse<Void> response = epubAnnotationService.deleteAnnotation(slug, annotationId);
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════════
    // BOOKMARKS
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/books/{slug}/epub-bookmarks
     *
     * Body: { cfi, label? }
     * Auth: wajib login
     */
    @PostMapping("/epub-bookmarks")
    public ResponseEntity<DataResponse<EpubBookmarkResponse>> addBookmark(
            @PathVariable String slug,
            @Valid @RequestBody EpubBookmarkRequest request) {

        DataResponse<EpubBookmarkResponse> response = epubAnnotationService.addBookmark(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/books/{slug}/epub-bookmarks/{bookmarkId}
     *
     * Auth: wajib login, hanya bisa hapus milik sendiri
     */
    @DeleteMapping("/epub-bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<Void>> deleteBookmark(
            @PathVariable String slug,
            @PathVariable Long bookmarkId) {

        DataResponse<Void> response = epubAnnotationService.deleteBookmark(slug, bookmarkId);
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════════
    // READING SESSION
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/books/{slug}/reading/epub-session
     *
     * Merekam sesi baca EPUB secara lengkap saat user meninggalkan halaman.
     * Dikirim via dua jalur: fetch+keepalive (beforeunload) dan React cleanup.
     *
     * Body: EpubSessionRequest yang sudah include chapterLabel, chapterIndex, totalChapters
     */
    @PostMapping("/reading/epub-session")
    public ResponseEntity<DataResponse<Void>> recordEpubSession(
            @PathVariable String slug,
            @RequestBody EpubSessionRequest request) {

        DataResponse<Void> response = epubAnnotationService.recordEpubSession(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/books/{slug}/reading/start
     *
     * Dipanggil saat EpubReaderPage pertama mount.
     * Menggunakan EpubStartReadingRequest (bukan StartReadingRequest) karena:
     *   - Tidak perlu chapterNumber (EPUB tidak pakai nomor chapter dari DB)
     *   - Menerima chapterLabel, chapterIndex, totalChapters dari TOC .epub
     *
     * Auth: wajib login
     */
    @PostMapping("/reading/start")
    public ResponseEntity<DataResponse<EpubStartReadingResponse>> startReading(
            @PathVariable String slug,
            @Valid @RequestBody EpubStartReadingRequest request) {

        DataResponse<EpubStartReadingResponse> response = epubAnnotationService.startReading(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/books/{slug}/reading/end
     *
     * No-op untuk EPUB — data sesi direkam via /reading/epub-session.
     * Endpoint tetap tersedia untuk konsistensi API.
     */
    @PostMapping("/reading/end")
    public ResponseEntity<DataResponse<Void>> endReading(
            @PathVariable String slug,
            @Valid @RequestBody EndReadingRequest request) {

        DataResponse<Void> response = epubAnnotationService.endReading(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/books/{slug}/reading/heartbeat
     *
     * Heartbeat untuk EPUB — saat ini hanya acknowledge, tidak disimpan.
     * Data aktual direkam di session saat user keluar halaman.
     */
    @PostMapping("/reading/heartbeat")
    public ResponseEntity<DataResponse<Void>> readingHeartbeat(
            @PathVariable String slug,
            @Valid @RequestBody ReadingHeartbeatRequest request) {

        DataResponse<Void> response = new DataResponse<>("Success", "Heartbeat received", 200, null);
        return ResponseEntity.ok(response);
    }
}