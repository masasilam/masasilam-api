package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Bookmark;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookmarkMapper {

    @Insert("INSERT INTO bookmarks (user_id, book_id, chapter_number, chapter_title, chapter_slug, position, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{chapterTitle}, #{chapterSlug}, #{position}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBookmark(Bookmark bookmark);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} ORDER BY page ASC")
    List<Bookmark> findBookmarksByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} AND position = #{position}")
    Bookmark findBookmarkByUserBookAndPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("position") String position);

    @Delete("DELETE FROM bookmarks WHERE id = #{id}")
    void deleteBookmark(@Param("id") Long id);

    @Update("UPDATE bookmarks SET title = #{title}, description = #{description}, color = #{color} WHERE id = #{id}")
    void updateBookmark(Bookmark bookmark);

    Bookmark findBookmarkById(@Param("id") Long id);

    List<Bookmark> findByUserBookAndPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("page") Integer page);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<Bookmark> findByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} " +
            "AND created_at >= #{since} " +
            "ORDER BY created_at DESC")
    List<Bookmark> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);


    @Select("SELECT COUNT(*) FROM bookmarks WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM bookmarks " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByBookAndUser(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM bookmarks b " +
            "INNER JOIN chapter_progress cp ON b.book_id = cp.book_id " +
            "WHERE b.user_id = #{userId} " +
            "AND cp.user_id = #{userId} " +
            "AND cp.is_completed = false")
    Integer countPendingByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM bookmarks WHERE id = #{id}")
    Bookmark findById(@Param("id") Long id);

    @Insert("INSERT INTO bookmarks " +
            "(user_id, book_id, chapter_number, paragraph_index, note, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{paragraphIndex}, " +
            "#{note}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Bookmark bookmark);

    @Update("UPDATE bookmarks SET note = #{note}, updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(Bookmark bookmark);

    @Delete("DELETE FROM bookmarks WHERE id = #{id}")
    void delete(@Param("id") Long id);

    @Select("SELECT * FROM bookmarks " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Bookmark> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);


    @Select("SELECT COUNT(*) FROM bookmarks " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY chapter_number, created_at DESC")
    List<Bookmark> findByUserAndBook(@Param("userId") Long userId,
                                     @Param("bookId") Long bookId);

    @Select("SELECT * FROM bookmarks WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND chapter_number = #{chapterNumber} ORDER BY created_at DESC")
    List<Bookmark> findByUserBookAndChapter(@Param("userId") Long userId,
                                            @Param("bookId") Long bookId,
                                            @Param("chapterNumber") Integer chapterNumber);
}