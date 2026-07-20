package com.masasilam.app.service.social;

import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;

public interface SocialAnnotationService {
    DataResponse<SocialAnnotationResponse> publishAnnotation(PublishAnnotationRequest request);
    DataResponse<SocialAnnotationResponse> updateAnnotation(Long annotationId, PublishAnnotationRequest request);
    DataResponse<Void> deleteAnnotation(Long annotationId);
    DataResponse<SocialAnnotationResponse> getAnnotationDetail(Long annotationId);
    DatatableResponse<SocialAnnotationResponse> getPublicAnnotations(int page, int limit);
    DatatableResponse<SocialAnnotationResponse> getFollowingAnnotations(int page, int limit);
    DatatableResponse<SocialAnnotationResponse> getUserAnnotations(Long userId, int page, int limit);
    DatatableResponse<SocialAnnotationResponse> getEntityAnnotations(String entityType, Long entityId, int page, int limit);
    DataResponse<Void> likeAnnotation(Long annotationId);
    DataResponse<Void> unlikeAnnotation(Long annotationId);
    DataResponse<Void> reshareAnnotation(Long annotationId);
    DataResponse<AnnotationCommentResponse> commentOnAnnotation(Long annotationId, AnnotationCommentRequest request);
    DataResponse<AnnotationCommentResponse> updateAnnotationComment(Long commentId, AnnotationCommentRequest request);
    DataResponse<Void> deleteAnnotationComment(Long commentId);
    DatatableResponse<AnnotationCommentResponse> getAnnotationComments(Long annotationId, int page, int limit);
}