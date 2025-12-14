package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChapterAudioMapper {
    ChapterAudio findChapterAudio(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );
}