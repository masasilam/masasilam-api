package com.naskah.app.service;

import com.naskah.app.model.dto.request.*;
import com.naskah.app.model.dto.response.*;

public interface EpubAnnotationService {
    DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug);
    DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request);
    DataResponse<Void> deleteAnnotation(String bookSlug, Long annotationId);
    DataResponse<EpubBookmarkResponse> addBookmark(String bookSlug, EpubBookmarkRequest request);
    DataResponse<Void> deleteBookmark(String bookSlug, Long bookmarkId);
    DataResponse<EpubStartReadingResponse> startReading(String slug, EpubStartReadingRequest request);
    DataResponse<Void> endReading(String slug, EndReadingRequest request);
    DataResponse<Void> recordEpubSession(String slug, EpubSessionRequest request);
}