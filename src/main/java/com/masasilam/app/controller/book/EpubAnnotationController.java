package com.masasilam.app.controller.book;

import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.book.EpubAnnotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/books/{slug}")
@RequiredArgsConstructor
public class EpubAnnotationController {
    private final EpubAnnotationService epubAnnotationService;

    @GetMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationsBundleResponse>> getAll(@PathVariable String slug) {
        DataResponse<EpubAnnotationsBundleResponse> response = epubAnnotationService.getAll(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/epub-annotations")
    public ResponseEntity<DataResponse<EpubAnnotationResponse>> addAnnotation(@PathVariable String slug, @Valid @RequestBody EpubAnnotationRequest request) {
        DataResponse<EpubAnnotationResponse> response = epubAnnotationService.addAnnotation(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/epub-annotations/{annotationId}")
    public ResponseEntity<DataResponse<Void>> deleteAnnotation(@PathVariable String slug, @PathVariable Long annotationId) {
        DataResponse<Void> response = epubAnnotationService.deleteAnnotation(slug, annotationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/epub-bookmarks")
    public ResponseEntity<DataResponse<EpubBookmarkResponse>> addBookmark(@PathVariable String slug, @Valid @RequestBody EpubBookmarkRequest request) {
        DataResponse<EpubBookmarkResponse> response = epubAnnotationService.addBookmark(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/epub-bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<Void>> deleteBookmark(@PathVariable String slug, @PathVariable Long bookmarkId) {
        DataResponse<Void> response = epubAnnotationService.deleteBookmark(slug, bookmarkId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reading/epub-session")
    public ResponseEntity<DataResponse<Void>> recordEpubSession(@PathVariable String slug, @RequestBody EpubSessionRequest request) {
        DataResponse<Void> response = epubAnnotationService.recordEpubSession(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reading/start")
    public ResponseEntity<DataResponse<EpubStartReadingResponse>> startReading(@PathVariable String slug, @Valid @RequestBody EpubStartReadingRequest request) {
        DataResponse<EpubStartReadingResponse> response = epubAnnotationService.startReading(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reading/end")
    public ResponseEntity<DataResponse<Void>> endReading(@PathVariable String slug, @Valid @RequestBody EndReadingRequest request) {
        DataResponse<Void> response = epubAnnotationService.endReading(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reading/heartbeat")
    public ResponseEntity<DataResponse<Void>> readingHeartbeat(@PathVariable String slug, @Valid @RequestBody ReadingHeartbeatRequest request) {
        DataResponse<Void> response = new DataResponse<>("Success", "Heartbeat received", 200, null);
        return ResponseEntity.ok(response);
    }
}