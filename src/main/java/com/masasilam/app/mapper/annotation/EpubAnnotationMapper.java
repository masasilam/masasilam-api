package com.masasilam.app.mapper.annotation;

import com.masasilam.app.model.entity.EpubAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EpubAnnotationMapper {
    List<EpubAnnotation> findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insert(EpubAnnotation annotation);
    EpubAnnotation findById(@Param("id") Long id);
    void deleteById(@Param("id") Long id);
    Integer countByUser(@Param("userId") Long userId);
    Integer countHighlightsByUser(@Param("userId") Long userId);
    Integer countNotesByUser(@Param("userId") Long userId);
    Integer countByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<EpubAnnotation> findByUserPaged(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    List<EpubAnnotation> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
    List<EpubAnnotation> findByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    List<EpubAnnotation> findByUser(@Param("userId") Long userId);
    List<EpubAnnotation> findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}