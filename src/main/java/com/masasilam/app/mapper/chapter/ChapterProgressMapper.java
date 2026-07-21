package com.masasilam.app.mapper.chapter;

import com.masasilam.app.model.entity.ChapterProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterProgressMapper {
    ChapterProgress findProgress(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    List<ChapterProgress> findAllByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void updateProgress(ChapterProgress progress);
    void insertProgress(ChapterProgress progress);
}