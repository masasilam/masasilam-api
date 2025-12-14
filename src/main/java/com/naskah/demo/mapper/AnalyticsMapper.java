package com.naskah.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface AnalyticsMapper {

    void updateHighlightHeatmap(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber,
            @Param("count") int count
    );

    void updateNoteHeatmap(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber,
            @Param("count") int count
    );

    Map<String, Object> getChapterStats(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );

    void updateReadingHeatmap(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber,
            @Param("count") int count
    );
}