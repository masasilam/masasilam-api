package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface BookChapterMapper {

    /**
     * Insert new chapter
     */
    void insertChapter(BookChapter chapter);

    /**
     * Find chapter by book ID and chapter number
     */
    BookChapter findChapterByNumber(@Param("bookId") Long bookId,
                                    @Param("chapterNumber") Integer chapterNumber);

    BookChapter findChapterById(@Param("id") Long id);

    /**
     * Find all chapters by book ID
     */
    List<BookChapter> findChaptersByBookId(@Param("bookId") Long bookId);

    /**
     * Search in book content
     */
    List<BookChapter> searchInBook(@Param("bookId") Long bookId,
                                   @Param("query") String query);

    /**
     * Delete all chapters by book ID
     */
    void deleteChaptersByBookId(@Param("bookId") Long bookId);

    /**
     * Update chapter
     */
    void updateChapter(BookChapter chapter);

    /**
     * Count chapters by book ID
     */
    int countChaptersByBookId(@Param("bookId") Long bookId);

    List<BookChapter> findSubChapters(@Param("parentChapterId") Long parentChapterId);

    /**
     * ✅ Find chapter by slug and parent ID (XML-based for PostgreSQL NULL handling)
     */
    BookChapter findChapterBySlugAndParent(
            @Param("bookId") Long bookId,
            @Param("slug") String slug,
            @Param("parentId") Long parentId
    );

    List<BookChapter> findChaptersByBookAndNumbers(@Param("bookId") Long bookId, @Param("chapterNumbers") Set<Integer> chapterNumbers);

    List<BookChapter> findChaptersByIds(@Param("ids") Set<Long> ids);

    String getChapterTitle(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    String getChapterSlug(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
}