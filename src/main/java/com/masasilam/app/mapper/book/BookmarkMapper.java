package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.Bookmark;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    @Insert("INSERT INTO bookmarks (user_id, book_id, chapter_number, chapter_title, chapter_slug, position, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{chapterTitle}, #{chapterSlug}, #{position}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBookmark(Bookmark bookmark);

    @Delete("DELETE FROM bookmarks WHERE id = #{id}")
    void deleteBookmark(@Param("id") Long id);


    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY chapter_number, created_at DESC")
    List<Bookmark> findByUserAndBook(@Param("userId") Long userId,
                                     @Param("bookId") Long bookId);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND chapter_number = #{chapterNumber} ORDER BY created_at DESC")
    List<Bookmark> findByUserBookAndChapter(@Param("userId") Long userId,
                                            @Param("bookId") Long bookId,
                                            @Param("chapterNumber") Integer chapterNumber);
    Bookmark findBookmarkById(@Param("id") Long id);
}