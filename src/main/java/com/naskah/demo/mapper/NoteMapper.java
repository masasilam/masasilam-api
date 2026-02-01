package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Note;
import com.naskah.demo.model.entity.NoteComment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoteMapper {

    @Insert("INSERT INTO notes (user_id, book_id, chapter_number, chapter_title, chapter_slug, position, content, selected_text, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{chapterTitle}, #{chapterSlug}, #{position}, #{content}, #{selectedText}, #{createdAt}, #{updatedAt})")
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

    @Select("SELECT * FROM notes WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND chapter_number = #{chapterNumber} ORDER BY created_at DESC")
    List<Note> findByUserBookAndChapter(@Param("userId") Long userId,
                                        @Param("bookId") Long bookId,
                                        @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT * FROM notes WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<Note> findByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM notes WHERE user_id = #{userId} " +
            "AND created_at >= #{since} " +
            "ORDER BY created_at DESC")
    List<Note> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Select("SELECT * FROM notes WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY chapter_number, created_at DESC")
    List<Note> findByUserAndBook(@Param("userId") Long userId,
                                 @Param("bookId") Long bookId);

    @Select("SELECT COUNT(*) FROM notes WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM notes " + "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByBookAndUser(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM notes " + "WHERE user_id = #{userId}")
    Integer countDraftsByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM notes WHERE id = #{id}")
    Note findById(@Param("id") Long id);

    @Insert("INSERT INTO notes " +
            "(user_id, book_id, chapter_number, note_text, note_type, " +
            "is_draft, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{noteText}, " +
            "#{noteType}, #{isDraft}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Note note);

    @Update("UPDATE notes SET " +
            "note_text = #{noteText}, " +
            "note_type = #{noteType}, " +
            "is_draft = #{isDraft}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(Note note);

    @Delete("DELETE FROM notes WHERE id = #{id}")
    void delete(@Param("id") Long id);

    @Select("SELECT * FROM notes " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Note> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM notes " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);
}