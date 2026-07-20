package com.naskah.app.mapper.social;

import com.naskah.app.model.entity.social.*;
import com.naskah.app.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SocialAnnotationMapper {
    void insert(SocialAnnotation annotation);
    void update(SocialAnnotation annotation);
    void softDelete(Long id);
    SocialAnnotation findById(Long id);

    List<SocialAnnotationResponse> findPublicAnnotations(@Param("currentUserId") Long currentUserId,
                                                         @Param("offset") int offset, @Param("limit") int limit);
    List<SocialAnnotationResponse> findFollowingAnnotations(@Param("userId") Long userId,
                                                            @Param("offset") int offset, @Param("limit") int limit);
    List<SocialAnnotationResponse> findByUser(@Param("profileUserId") Long profileUserId,
                                              @Param("currentUserId") Long currentUserId,
                                              @Param("offset") int offset, @Param("limit") int limit);
    List<SocialAnnotationResponse> findByEntity(@Param("entityType") String entityType,
                                                @Param("entityId") Long entityId,
                                                @Param("currentUserId") Long currentUserId,
                                                @Param("offset") int offset, @Param("limit") int limit);
    int countByUser(Long userId);
    int countByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    int countPublic();

    // Likes
    void insertLike(@Param("annotationId") Long annotationId, @Param("userId") Long userId);
    void deleteLike(@Param("annotationId") Long annotationId, @Param("userId") Long userId);
    boolean isLiked(@Param("annotationId") Long annotationId, @Param("userId") Long userId);
    void incrementLikeCount(Long annotationId);
    void decrementLikeCount(Long annotationId);
    void incrementReshareCount(Long annotationId);

    // Comments
    void insertComment(SocialAnnotationComment comment);
    void updateComment(SocialAnnotationComment comment);
    void softDeleteComment(Long id);
    SocialAnnotationComment findCommentById(Long id);
    List<AnnotationCommentResponse> findCommentsByAnnotation(@Param("annotationId") Long annotationId,
                                                             @Param("currentUserId") Long currentUserId);
    void incrementCommentCount(Long annotationId);
    void decrementCommentCount(Long annotationId);
}