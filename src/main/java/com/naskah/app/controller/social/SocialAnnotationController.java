package com.naskah.app.controller.social;

import com.naskah.app.model.dto.request.social.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;
import com.naskah.app.service.social.SocialAnnotationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/annotations")
@RequiredArgsConstructor
public class SocialAnnotationController {
    private final SocialAnnotationService annotationService;

    @GetMapping("/public")
    public ResponseEntity<DatatableResponse<SocialAnnotationResponse>> getPublicAnnotations(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(annotationService.getPublicAnnotations(page, limit));
    }

    @GetMapping("/following")
    public ResponseEntity<DatatableResponse<SocialAnnotationResponse>> getFollowingAnnotations(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                               @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(annotationService.getFollowingAnnotations(page, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DatatableResponse<SocialAnnotationResponse>> getUserAnnotations(@PathVariable Long userId,
                                                                                          @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                          @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(annotationService.getUserAnnotations(userId, page, limit));
    }

    @GetMapping("/entity")
    public ResponseEntity<DatatableResponse<SocialAnnotationResponse>> getEntityAnnotations(@RequestParam String entityType,
                                                                                            @RequestParam Long entityId,
                                                                                            @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(annotationService.getEntityAnnotations(entityType, entityId, page, limit));
    }

    @GetMapping("/{annotationId}")
    public ResponseEntity<DataResponse<SocialAnnotationResponse>> getAnnotationDetail(@PathVariable Long annotationId) {
        return ResponseEntity.ok(annotationService.getAnnotationDetail(annotationId));
    }

    @PostMapping
    public ResponseEntity<DataResponse<SocialAnnotationResponse>> publishAnnotation(@Valid @RequestBody PublishAnnotationRequest request) {
        DataResponse<SocialAnnotationResponse> response = annotationService.publishAnnotation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{annotationId}")
    public ResponseEntity<DataResponse<SocialAnnotationResponse>> updateAnnotation(@PathVariable Long annotationId,
                                                                                   @RequestBody PublishAnnotationRequest request) {
        return ResponseEntity.ok(annotationService.updateAnnotation(annotationId, request));
    }

    @DeleteMapping("/{annotationId}")
    public ResponseEntity<DataResponse<Void>> deleteAnnotation(@PathVariable Long annotationId) {
        return ResponseEntity.ok(annotationService.deleteAnnotation(annotationId));
    }

    @PostMapping("/{annotationId}/like")
    public ResponseEntity<DataResponse<Void>> likeAnnotation(@PathVariable Long annotationId) {
        return ResponseEntity.ok(annotationService.likeAnnotation(annotationId));
    }

    @DeleteMapping("/{annotationId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeAnnotation(@PathVariable Long annotationId) {
        return ResponseEntity.ok(annotationService.unlikeAnnotation(annotationId));
    }

    @PostMapping("/{annotationId}/reshare")
    public ResponseEntity<DataResponse<Void>> reshareAnnotation(@PathVariable Long annotationId) {
        return ResponseEntity.ok(annotationService.reshareAnnotation(annotationId));
    }

    @GetMapping("/{annotationId}/comments")
    public ResponseEntity<DatatableResponse<AnnotationCommentResponse>> getAnnotationComments(@PathVariable Long annotationId,
                                                                                              @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                              @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(annotationService.getAnnotationComments(annotationId, page, limit));
    }

    @PostMapping("/{annotationId}/comments")
    public ResponseEntity<DataResponse<AnnotationCommentResponse>> commentOnAnnotation(@PathVariable Long annotationId,
                                                                                       @Valid @RequestBody AnnotationCommentRequest request) {
        DataResponse<AnnotationCommentResponse> response = annotationService.commentOnAnnotation(annotationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<AnnotationCommentResponse>> updateAnnotationComment(@PathVariable Long commentId,
                                                                                           @Valid @RequestBody AnnotationCommentRequest request) {
        return ResponseEntity.ok(annotationService.updateAnnotationComment(commentId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<DataResponse<Void>> deleteAnnotationComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(annotationService.deleteAnnotationComment(commentId));
    }
}