package com.masasilam.app.mapper.annotation;

import com.masasilam.app.model.entity.Highlight;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HighlightMapper {
    void insertHighlight(Highlight highlight);
    Highlight findHighlightById(@Param("id") Long id);
    void deleteHighlight(@Param("id") Long id);
    List<Highlight> findByUserBookAndPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("page") Integer page);
    List<Highlight> findByUser(@Param("userId") Long userId);
    List<Highlight> findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    Integer countByUser(@Param("userId") Long userId);
    Highlight findById(@Param("id") Long id);
    void insert(Highlight highlight);
    void update(Highlight highlight);
    void delete(@Param("id") Long id);
    List<Highlight> findByUserBookAndChapter(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
}