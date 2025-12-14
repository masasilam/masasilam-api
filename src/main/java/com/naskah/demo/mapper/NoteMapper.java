package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Note;
import com.naskah.demo.model.entity.NoteComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteMapper {

    @Insert("INSERT INTO notes (user_id, book_id, page, position, title, content, color, is_private, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{page}, #{position}, #{title}, #{content}, #{color}, #{isPrivate}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNote(Note note);

    @Select("SELECT * FROM notes WHERE user_id = #{userId} AND book_id = #{bookId} ORDER BY page ASC, created_at DESC")
    List<Note> findNotesByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT * FROM notes WHERE id = #{id}")
    Note findNoteById(@Param("id") Long id);

    @Update("UPDATE notes SET title = #{title}, content = #{content}, color = #{color}, is_private = #{isPrivate}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateNote(Note note);

    @Delete("DELETE FROM notes WHERE id = #{id}")
    void deleteNote(@Param("id") Long id);

    // Note comments
    @Insert("INSERT INTO note_comments (note_id, user_id, content, parent_comment_id, created_at) " +
            "VALUES (#{noteId}, #{userId}, #{content}, #{parentCommentId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNoteComment(NoteComment comment);

    @Select("SELECT * FROM note_comments WHERE note_id = #{noteId} ORDER BY created_at ASC")
    List<NoteComment> findCommentsByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT COUNT(*) FROM note_comments WHERE note_id = #{noteId}")
    Integer countCommentsByNoteId(@Param("noteId") Long noteId);

    List<Note> findByUserBookAndPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("page") Integer page);
}