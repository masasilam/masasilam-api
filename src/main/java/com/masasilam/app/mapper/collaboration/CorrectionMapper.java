package com.masasilam.app.mapper.collaboration;

import com.masasilam.app.model.entity.ContentCorrection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CorrectionMapper {
    void insertCorrection(ContentCorrection correction);
    ContentCorrection findById(@Param("id") Long id);
    List<ContentCorrection> findByStatus(@Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);
    int countByStatus(@Param("status") String status);
    List<ContentCorrection> findPendingByChapterId(@Param("chapterId") Long chapterId);
    List<Integer> findPendingPositionsByChapterId(@Param("chapterId") Long chapterId);
    int countDuplicateByUserAndChapter(@Param("submittedBy") Long submittedBy, @Param("chapterId") Long chapterId, @Param("originalText") String originalText);
    void updateCorrection(ContentCorrection correction);
    List<String> findApprovedEditorsByBookId(@Param("bookId") Long bookId);
    List<ContentCorrection> findByStatusAndUser(@Param("status") String status, @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    int countByStatusAndUser(@Param("status") String status, @Param("userId") Long userId);
    List<ContentCorrection> findPendingByBookAndUser(@Param("bookId") Long bookId, @Param("userId") Long userId);
}