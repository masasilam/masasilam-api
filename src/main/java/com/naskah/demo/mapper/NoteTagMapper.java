package com.naskah.demo.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteTagMapper {

    @Insert("INSERT INTO note_tags (note_id, tag_name, created_at) VALUES (#{noteId}, #{tagName}, #{createdAt})")
    void insertNoteTag(@Param("noteId") Long noteId, @Param("tagName") String tagName, @Param("createdAt") java.time.LocalDateTime createdAt);

    @Select("SELECT tag_name FROM note_tags WHERE note_id = #{noteId}")
    List<String> findTagsByNoteId(@Param("noteId") Long noteId);

    @Delete("DELETE FROM note_tags WHERE note_id = #{noteId}")
    void deleteNoteTagsByNoteId(@Param("noteId") Long noteId);

    @Delete("DELETE FROM note_tags WHERE note_id = #{noteId} AND tag_name = #{tagName}")
    void deleteNoteTag(@Param("noteId") Long noteId, @Param("tagName") String tagName);

    @Select("SELECT tag_name, COUNT(*) as frequency FROM note_tags " +
            "WHERE note_id IN (SELECT id FROM notes WHERE user_id = #{userId} AND book_id = #{bookId}) " +
            "GROUP BY tag_name ORDER BY frequency DESC")
    List<java.util.Map<String, Object>> getTagFrequency(@Param("userId") Long userId, @Param("bookId") Long bookId);
}