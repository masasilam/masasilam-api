package com.naskah.app.service.zine;

import com.naskah.app.model.dto.request.ZineReadingProgressRequest;
import com.naskah.app.model.dto.request.ZineReadingSessionRequest;
import com.naskah.app.model.dto.response.DataResponse;
import com.naskah.app.model.dto.response.ZineReadingProgressResponse;
import com.naskah.app.model.dto.response.ZineReadingSessionResponse;

public interface ZineReadingService {

    /** Dipanggil saat EPUB reader selesai satu sesi baca */
    DataResponse<ZineReadingSessionResponse> saveReadingSession(String slug, ZineReadingSessionRequest request);

    /** Dipanggil setiap kali CFI berubah (page-turn) */
    DataResponse<ZineReadingProgressResponse> saveOrUpdateProgress(String slug, ZineReadingProgressRequest request);

    /** Ambil progress terakhir user untuk zine tertentu (resume baca) */
    DataResponse<ZineReadingProgressResponse> getProgress(String slug);
}