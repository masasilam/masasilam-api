package com.naskah.app.mapper;

import com.naskah.app.model.entity.ChapterAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChapterAudioMapper {
    ChapterAudio findChapterAudio(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );
}