package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterListeningProgress;
import com.naskah.demo.model.entity.ChapterProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterProgressMapper {

    ChapterProgress findProgress(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );

    ChapterListeningProgress findListeningProgress(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );

    void insertListeningProgress(ChapterListeningProgress progress);

    void updateListeningProgress(ChapterListeningProgress progress);

    List<ChapterProgress> findAllByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    void updateProgress(ChapterProgress progress);

    void insertProgress(ChapterProgress progress);
}