package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Bookmark;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    @Insert("INSERT INTO bookmarks (user_id, book_id, page, position, title, description, color, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{page}, #{position}, #{title}, #{description}, #{color}, #{createdAt})")
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
}