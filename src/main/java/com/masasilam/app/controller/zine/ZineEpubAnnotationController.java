package com.masasilam.app.controller.zine;

import com.masasilam.app.model.dto.request.EpubAnnotationRequest;
import com.masasilam.app.model.dto.request.EpubBookmarkRequest;
import com.masasilam.app.model.dto.request.EpubSessionRequest;
import com.masasilam.app.model.dto.request.EpubStartReadingRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.book.EpubAnnotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/zines/{slug}")
@RequiredArgsConstructor
public class ZineEpubAnnotationController {
    private final EpubAnnotationService epubAnnotationService;

    @GetMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationsBundleResponse>> getAll(@PathVariable String slug) {
        return ResponseEntity.ok(epubAnnotationService.getAll(slug));
    }

    @PostMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationResponse>> addAnnotation(@PathVariable String slug,
                                                                              @Valid @RequestBody EpubAnnotationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epubAnnotationService.addAnnotation(slug, request));
    }

    @DeleteMapping("/epub-annotations/{annotationId}")
    public ResponseEntity<DataResponse<Void>> deleteAnnotation(@PathVariable String slug,
                                                               @PathVariable Long annotationId) {
        return ResponseEntity.ok(epubAnnotationService.deleteAnnotation(slug, annotationId));
    }

    @PostMapping("/epub-bookmarks")
    public ResponseEntity<DataResponse<EpubBookmarkResponse>> addBookmark(@PathVariable String slug,
                                                                          @Valid @RequestBody EpubBookmarkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epubAnnotationService.addBookmark(slug, request));
    }

    @DeleteMapping("/epub-bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<Void>> deleteBookmark(@PathVariable String slug,
                                                             @PathVariable Long bookmarkId) {
        return ResponseEntity.ok(epubAnnotationService.deleteBookmark(slug, bookmarkId));
    }

    @PostMapping("/reading/epub-session")
    public ResponseEntity<DataResponse<Void>> recordEpubSession(@PathVariable String slug,
                                                                @RequestBody EpubSessionRequest request) {
        return ResponseEntity.ok(epubAnnotationService.recordEpubSession(slug, request));
    }

    @PostMapping("/reading/start")
    public ResponseEntity<DataResponse<EpubStartReadingResponse>> startReading(@PathVariable String slug,
                                                                               @Valid @RequestBody EpubStartReadingRequest request) {
        return ResponseEntity.ok(epubAnnotationService.startReading(slug, request));
    }
}