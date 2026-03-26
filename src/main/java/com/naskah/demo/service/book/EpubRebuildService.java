// ============================================================
// FILE 1: service/book/EpubRebuildService.java
// ============================================================
package com.naskah.demo.service.book;

/**
 * Service untuk rebuild file .epub dari data chapter di DB.
 *
 * Kenapa terpisah dari EpubServiceImpl:
 *   EpubServiceImpl  : epub → DB  (arah import)
 *   EpubRebuildService: DB → epub (arah export/rebuild)
 *   Dua arah yang berbeda, single responsibility.
 *
 * Dipanggil oleh CorrectionServiceImpl setelah admin approve koreksi.
 * Berjalan di background thread (@Async) — tidak memblokir response admin.
 *
 * Hasil: file .epub baru di-upload ke Cloudinary dengan public_id SAMA
 * (overwrite) sehingga book.file_url tidak berubah tapi kontennya fresh.
 */
public interface EpubRebuildService {

    /**
     * Rebuild epub dari DB secara async.
     * Method ini langsung return — proses berjalan di background.
     *
     * Flow:
     *  1. Ambil semua chapter dari DB
     *  2. Generate .epub bytes (epublib)
     *  3. Upload ke Cloudinary dengan overwrite=true
     *  4. Update book.epub_generated_at
     *
     * @param bookId ID buku yang epub-nya perlu di-rebuild
     */
    void rebuildAsync(Long bookId);
}