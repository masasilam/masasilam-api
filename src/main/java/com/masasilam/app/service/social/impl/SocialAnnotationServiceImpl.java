package com.masasilam.app.service.social.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.mapper.social.SocialAnnotationMapper;
import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.service.social.*;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAnnotationServiceImpl implements SocialAnnotationService {
    private final SocialAnnotationMapper annotationMapper;
    private final UserMapper userMapper;
    private final ActivityFeedService feedService;
    private final NotificationService notificationService;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String PUBLIC = "public";

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long currentUserIdOrNull() {
        try {
            String u = headerHolder.getUsername();
            if (u == null || u.isBlank()) return null;
            User user = userMapper.findUserByUsername(u);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public DataResponse<SocialAnnotationResponse> publishAnnotation(PublishAnnotationRequest request) {
        User me = requireAuth();

        SocialAnnotation annotation = new SocialAnnotation();
        annotation.setUserId(me.getId());
        annotation.setSourceAnnotationId(request.getSourceAnnotationId());
        annotation.setEntityType(request.getEntityType());
        annotation.setEntityId(request.getEntityId());
        annotation.setEntitySlug(request.getEntitySlug() != null ? request.getEntitySlug() : "");
        annotation.setEntityTitle(request.getEntityTitle() != null ? request.getEntityTitle() : "");
        annotation.setCfi(request.getCfi());
        annotation.setSelectedText(request.getSelectedText());
        annotation.setColor(request.getColor() != null ? request.getColor() : "#FDE68A");
        annotation.setNote(request.getNote());
        annotation.setContextBefore(request.getContextBefore());
        annotation.setContextAfter(request.getContextAfter());
        annotation.setChapterLabel(request.getChapterLabel());
        annotation.setVisibility(request.getVisibility() != null ? request.getVisibility() : PUBLIC);
        annotationMapper.insert(annotation);

        feedService.publishActivity(me.getId(), "shared_annotation",
                request.getEntityType(), request.getEntityId(),
                request.getEntitySlug(), request.getEntityTitle(), null,
                "{\"annotationId\":" + annotation.getId() + "}",
                annotation.getVisibility());

        List<SocialAnnotationResponse> results = annotationMapper.findByUser(me.getId(), me.getId(), 0, 1);
        SocialAnnotationResponse response = results.isEmpty() ? new SocialAnnotationResponse() : results.get(0);
        return new DataResponse<>(SUCCESS, "Annotation published", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<SocialAnnotationResponse> updateAnnotation(Long annotationId, PublishAnnotationRequest request) {
        User me = requireAuth();
        SocialAnnotation ann = annotationMapper.findById(annotationId);
        if (ann == null || !ann.getIsActive()) throw new DataNotFoundException();
        if (!ann.getUserId().equals(me.getId())) throw new ForbiddenException();

        if (request.getNote() != null) ann.setNote(request.getNote());
        if (request.getColor() != null) ann.setColor(request.getColor());
        if (request.getVisibility() != null) ann.setVisibility(request.getVisibility());
        annotationMapper.update(ann);

        List<SocialAnnotationResponse> results = annotationMapper.findByUser(me.getId(), me.getId(), 0, 50);
        SocialAnnotationResponse response = results.stream()
                .filter(r -> r.getId().equals(annotationId)).findFirst()
                .orElse(new SocialAnnotationResponse());
        return new DataResponse<>(SUCCESS, "Annotation updated", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteAnnotation(Long annotationId) {
        User me = requireAuth();
        SocialAnnotation ann = annotationMapper.findById(annotationId);
        if (ann == null) throw new DataNotFoundException();
        if (!ann.getUserId().equals(me.getId())) throw new ForbiddenException();
        annotationMapper.softDelete(annotationId);
        return new DataResponse<>(SUCCESS, "Annotation deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<SocialAnnotationResponse> getAnnotationDetail(Long annotationId) {
        Long currentUserId = currentUserIdOrNull();
        List<SocialAnnotationResponse> results = annotationMapper.findPublicAnnotations(currentUserId, 0, 1000);
        SocialAnnotationResponse response = results.stream()
                .filter(r -> r.getId().equals(annotationId)).findFirst()
                .orElseThrow(DataNotFoundException::new);
        List<AnnotationCommentResponse> comments = annotationMapper.findCommentsByAnnotation(annotationId, currentUserId);
        response.setRecentComments(comments);
        return new DataResponse<>(SUCCESS, "Annotation retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DatatableResponse<SocialAnnotationResponse> getPublicAnnotations(int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<SocialAnnotationResponse> items = annotationMapper.findPublicAnnotations(currentUserId, offset, limit);
        int total = annotationMapper.countPublic();
        return new DatatableResponse<>(SUCCESS, "Annotations retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, items));
    }

    @Override
    public DatatableResponse<SocialAnnotationResponse> getFollowingAnnotations(int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<SocialAnnotationResponse> items = annotationMapper.findFollowingAnnotations(me.getId(), offset, limit);
        return new DatatableResponse<>(SUCCESS, "Following annotations",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, items.size(), items));
    }

    @Override
    public DatatableResponse<SocialAnnotationResponse> getUserAnnotations(Long userId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<SocialAnnotationResponse> items = annotationMapper.findByUser(userId, currentUserId, offset, limit);
        int total = annotationMapper.countByUser(userId);
        return new DatatableResponse<>(SUCCESS, "User annotations",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, items));
    }

    @Override
    public DatatableResponse<SocialAnnotationResponse> getEntityAnnotations(String entityType, Long entityId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<SocialAnnotationResponse> items = annotationMapper.findByEntity(entityType, entityId, currentUserId, offset, limit);
        int total = annotationMapper.countByEntity(entityType, entityId);
        return new DatatableResponse<>(SUCCESS, "Entity annotations",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, items));
    }

    @Override
    @Transactional
    public DataResponse<Void> likeAnnotation(Long annotationId) {
        User me = requireAuth();
        if (annotationMapper.isLiked(annotationId, me.getId()))
            throw new BadRequestException("Already liked");
        annotationMapper.insertLike(annotationId, me.getId());
        annotationMapper.incrementLikeCount(annotationId);

        SocialAnnotation ann = annotationMapper.findById(annotationId);
        if (ann != null && !ann.getUserId().equals(me.getId())) {
            notificationService.sendNotification(ann.getUserId(), me.getId(),
                    "annotation_like", "ANNOTATION", annotationId,
                    me.getUsername() + " menyukai kutipanmu", "{}");
        }
        return new DataResponse<>(SUCCESS, "Annotation liked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unlikeAnnotation(Long annotationId) {
        User me = requireAuth();
        if (!annotationMapper.isLiked(annotationId, me.getId())) throw new DataNotFoundException();
        annotationMapper.deleteLike(annotationId, me.getId());
        annotationMapper.decrementLikeCount(annotationId);
        return new DataResponse<>(SUCCESS, "Annotation unliked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> reshareAnnotation(Long annotationId) {
        User me = requireAuth();
        SocialAnnotation original = annotationMapper.findById(annotationId);
        if (original == null) throw new DataNotFoundException();

        annotationMapper.incrementReshareCount(annotationId);

        SocialAnnotation reshare = new SocialAnnotation();
        reshare.setUserId(me.getId());
        reshare.setSourceAnnotationId(annotationId);
        reshare.setEntityType(original.getEntityType());
        reshare.setEntityId(original.getEntityId());
        reshare.setEntitySlug(original.getEntitySlug());
        reshare.setEntityTitle(original.getEntityTitle());
        reshare.setCfi(original.getCfi());
        reshare.setSelectedText(original.getSelectedText());
        reshare.setColor(original.getColor());
        reshare.setNote(original.getNote());
        reshare.setChapterLabel(original.getChapterLabel());
        reshare.setVisibility(PUBLIC);
        annotationMapper.insert(reshare);

        feedService.publishActivity(me.getId(), "shared_annotation",
                original.getEntityType(), original.getEntityId(),
                original.getEntitySlug(), original.getEntityTitle(), null,
                "{\"resharedFrom\":" + annotationId + "}", PUBLIC);
        return new DataResponse<>(SUCCESS, "Annotation reshared", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<AnnotationCommentResponse> commentOnAnnotation(Long annotationId, AnnotationCommentRequest request) {
        User me = requireAuth();
        SocialAnnotationComment comment = new SocialAnnotationComment();
        comment.setAnnotationId(annotationId);
        comment.setUserId(me.getId());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        annotationMapper.insertComment(comment);
        annotationMapper.incrementCommentCount(annotationId);

        SocialAnnotation ann = annotationMapper.findById(annotationId);
        if (ann != null && !ann.getUserId().equals(me.getId())) {
            notificationService.sendNotification(ann.getUserId(), me.getId(),
                    "annotation_comment", "ANNOTATION", annotationId,
                    me.getUsername() + " berkomentar pada kutipanmu", "{}");
        }

        AnnotationCommentResponse response = new AnnotationCommentResponse();
        response.setId(comment.getId());
        response.setAnnotationId(annotationId);
        response.setUserId(me.getId());
        response.setUsername(me.getUsername());
        response.setUserPhoto(me.getProfilePictureUrl());
        response.setContent(request.getContent());
        response.setParentId(request.getParentId());
        response.setIsOwner(true);
        return new DataResponse<>(SUCCESS, "Comment added", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<AnnotationCommentResponse> updateAnnotationComment(Long commentId, AnnotationCommentRequest request) {
        User me = requireAuth();
        SocialAnnotationComment comment = annotationMapper.findCommentById(commentId);
        if (comment == null || comment.getIsDeleted()) throw new DataNotFoundException();
        if (!comment.getUserId().equals(me.getId())) throw new ForbiddenException();
        comment.setContent(request.getContent());
        annotationMapper.updateComment(comment);

        AnnotationCommentResponse response = new AnnotationCommentResponse();
        response.setId(comment.getId());
        response.setContent(request.getContent());
        response.setIsOwner(true);
        return new DataResponse<>(SUCCESS, "Comment updated", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteAnnotationComment(Long commentId) {
        User me = requireAuth();
        SocialAnnotationComment comment = annotationMapper.findCommentById(commentId);
        if (comment == null) throw new DataNotFoundException();
        if (!comment.getUserId().equals(me.getId())) throw new ForbiddenException();
        annotationMapper.softDeleteComment(commentId);
        annotationMapper.decrementCommentCount(comment.getAnnotationId());
        return new DataResponse<>(SUCCESS, "Comment deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DatatableResponse<AnnotationCommentResponse> getAnnotationComments(Long annotationId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        List<AnnotationCommentResponse> comments = annotationMapper.findCommentsByAnnotation(annotationId, currentUserId);
        return new DatatableResponse<>(SUCCESS, "Comments retrieved",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, comments.size(), comments));
    }
}