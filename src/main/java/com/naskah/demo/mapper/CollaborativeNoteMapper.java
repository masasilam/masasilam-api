package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.CollaborativeNote;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CollaborativeNoteMapper {

    @Insert("INSERT INTO collaborative_notes (book_id, author_id, page, position, title, content, visibility, edit_count, last_edited_at, last_edited_by, created_at) " +
            "VALUES (#{bookId}, #{authorId}, #{page}, #{position}, #{title}, #{content}, #{visibility}, #{editCount}, #{lastEditedAt}, #{lastEditedBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCollaborativeNote(CollaborativeNote note);

    @Select("SELECT * FROM collaborative_notes WHERE book_id = #{bookId} AND visibility = 'PUBLIC' ORDER BY created_at DESC")
    List<CollaborativeNote> findPublicNotesByBook(@Param("bookId") Long bookId);

    @Select("SELECT cn.* FROM collaborative_notes cn " +
            "LEFT JOIN collaborative_note_collaborators cnc ON cn.id = cnc.note_id " +
            "WHERE cn.book_id = #{bookId} AND (cn.author_id = #{userId} OR cnc.user_id = #{userId} OR cn.visibility = 'PUBLIC') " +
            "ORDER BY cn.created_at DESC")
    List<CollaborativeNote> findAccessibleNotesByBook(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Select("SELECT * FROM collaborative_notes WHERE id = #{id}")
    CollaborativeNote findCollaborativeNoteById(@Param("id") Long id);

    @Update("UPDATE collaborative_notes SET title = #{title}, content = #{content}, edit_count = edit_count + 1, " +
            "last_edited_at = #{lastEditedAt}, last_edited_by = #{lastEditedBy} WHERE id = #{id}")
    void updateCollaborativeNote(CollaborativeNote note);

    @Delete("DELETE FROM collaborative_notes WHERE id = #{id}")
    void deleteCollaborativeNote(@Param("id") Long id);

    // Collaborators management
    @Insert("INSERT INTO collaborative_note_collaborators (note_id, user_id, permission_level, added_at) " +
            "VALUES (#{noteId}, #{userId}, #{permissionLevel}, #{addedAt})")
    void addCollaborator(@Param("noteId") Long noteId, @Param("userId") Long userId,
                         @Param("permissionLevel") String permissionLevel, @Param("addedAt") java.time.LocalDateTime addedAt);

    @Select("SELECT u.username FROM users u " +
            "JOIN collaborative_note_collaborators cnc ON u.id = cnc.user_id " +
            "WHERE cnc.note_id = #{noteId}")
    List<String> findCollaboratorsByNoteId(@Param("noteId") Long noteId);

    @Delete("DELETE FROM collaborative_note_collaborators WHERE note_id = #{noteId} AND user_id = #{userId}")
    void removeCollaborator(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
