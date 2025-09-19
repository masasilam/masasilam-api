package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.VoiceNote;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VoiceNoteMapper {

    @Insert("INSERT INTO voice_notes (user_id, book_id, page, position, audio_url, duration, transcription, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{page}, #{position}, #{audioUrl}, #{duration}, #{transcription}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVoiceNote(VoiceNote voiceNote);

    @Select("SELECT * FROM voice_notes WHERE user_id = #{userId} AND book_id = #{bookId} ORDER BY page ASC, created_at DESC")
    List<VoiceNote> findVoiceNotesByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT * FROM voice_notes WHERE id = #{id}")
    VoiceNote findVoiceNoteById(@Param("id") Long id);

    @Update("UPDATE voice_notes SET transcription = #{transcription} WHERE id = #{id}")
    void updateVoiceNoteTranscription(@Param("id") Long id, @Param("transcription") String transcription);

    @Delete("DELETE FROM voice_notes WHERE id = #{id}")
    void deleteVoiceNote(@Param("id") Long id);
}