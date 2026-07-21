package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper {
    void insertNote(Note note);
    Note findNoteById(@Param("id") Long id);
    void deleteNote(@Param("id") Long id);
    List<Note> findByUserBookAndChapter(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    List<Note> findByUser(@Param("userId") Long userId);
    List<Note> findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    Integer countByUser(@Param("userId") Long userId);
    Note findById(@Param("id") Long id);
    void insert(Note note);
    void update(Note note);
    void delete(@Param("id") Long id);
}