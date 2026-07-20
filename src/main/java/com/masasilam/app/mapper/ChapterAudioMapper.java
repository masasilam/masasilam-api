package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.ChapterAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChapterAudioMapper {
    ChapterAudio findChapterAudio(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );
}