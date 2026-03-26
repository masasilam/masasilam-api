package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.EpubAnnotationRequest;
import com.naskah.demo.model.dto.request.EpubBookmarkRequest;
import com.naskah.demo.model.dto.response.*;

public interface EpubAnnotationService {

    /**
     * Ambil semua anotasi + bookmark milik user untuk buku ini.
     * Dipakai frontend saat pertama kali mount EpubReaderPage.
     */
    DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug);

    /** Tambah highlight/catatan baru. Mengembalikan objek dengan id dari DB. */
    DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request);

    /** Hapus annotation milik user. Akan throw UnauthorizedException jika bukan miliknya. */
    DataResponse<Void> deleteAnnotation(String bookSlug, Long annotationId);

    /** Tambah bookmark baru. */
    DataResponse<EpubBookmarkResponse> addBookmark(String bookSlug, EpubBookmarkRequest request);

    /** Hapus bookmark milik user. */
    DataResponse<Void> deleteBookmark(String bookSlug, Long bookmarkId);
}