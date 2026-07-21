package com.masasilam.app.mapper.zine;

import com.masasilam.app.model.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZineChapterMapper {
    void insertChapter(BookChapter chapter);
    void updateChapter(BookChapter chapter);
    BookChapter findChapterByNumber(@Param("zineId") Long zineId, @Param("chapterNumber") Integer chapterNumber);
    List<BookChapter> findChaptersByZineId(@Param("zineId") Long zineId);
    List<BookChapter> searchInZine(@Param("zineId") Long zineId, @Param("query") String query);
    void deleteChaptersByZineId(@Param("zineId") Long zineId);
    void deleteChapterById(@Param("id") Long id);
}