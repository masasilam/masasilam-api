package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Response untuk GET /api/books/{slug}/epub-annotations.
 * Menggabungkan annotations dan bookmarks dalam satu call
 * agar frontend tidak perlu dua round-trip saat pertama load.
 */
@Data
public class EpubAnnotationsBundleResponse {
    private List<EpubAnnotationResponse> annotations;
    private List<EpubBookmarkResponse> bookmarks;
}