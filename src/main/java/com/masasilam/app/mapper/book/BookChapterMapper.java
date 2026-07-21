package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface BookChapterMapper {
    void insertChapter(BookChapter chapter);
    BookChapter findChapterByNumber(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    BookChapter findChapterById(@Param("id") Long id);
    List<BookChapter> findChaptersByBookId(@Param("bookId") Long bookId);
    List<BookChapter> searchInBook(@Param("bookId") Long bookId, @Param("query") String query);
    void deleteChaptersByBookId(@Param("bookId") Long bookId);
    int countChaptersByBookId(@Param("bookId") Long bookId);
    List<BookChapter> findSubChapters(@Param("parentChapterId") Long parentChapterId);
    BookChapter findChapterBySlugAndParent(@Param("bookId") Long bookId, @Param("slug") String slug, @Param("parentId") Long parentId);
    List<BookChapter> findChaptersByBookAndNumbers(@Param("bookId") Long bookId, @Param("chapterNumbers") Set<Integer> chapterNumbers);
    List<BookChapter> findChaptersByIds(@Param("ids") Set<Long> ids);
    String getChapterTitle(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    String getChapterSlug(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    void updateChapter(BookChapter chapter);
    void deleteChapterById(@Param("id") Long id);
}