package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ContentCorrection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CorrectionMapper {

    void insertCorrection(ContentCorrection correction);

    ContentCorrection findById(@Param("id") Long id);

    List<ContentCorrection> findByStatus(
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countByStatus(@Param("status") String status);

    List<ContentCorrection> findPendingByChapterId(@Param("chapterId") Long chapterId);

    List<Integer> findPendingPositionsByChapterId(@Param("chapterId") Long chapterId);

    int countDuplicateByUserAndChapter(
            @Param("submittedBy") Long submittedBy,
            @Param("chapterId") Long chapterId,
            @Param("originalText") String originalText);

    void updateCorrection(ContentCorrection correction);

    /**
     * Ambil nama-nama user yang pernah meng-approve correction di buku ini.
     * Dipakai untuk inject editor ke kolofon epub dan content.opf.
     */
    List<String> findApprovedEditorsByBookId(@Param("bookId") Long bookId);
}