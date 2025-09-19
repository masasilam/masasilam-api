package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.AudioSync;
import com.naskah.demo.model.dto.response.AudioSyncResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AudioSyncMapper {

    @Insert("INSERT INTO audio_sync (book_id, page, text_position, audio_timestamp, text, is_verified, created_at) " +
            "VALUES (#{bookId}, #{page}, #{textPosition}, #{audioTimestamp}, #{text}, #{isVerified}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insertAudioSync(AudioSync audioSync);

    @Select("SELECT * FROM audio_sync WHERE book_id = #{bookId} AND page = #{page} ORDER BY audio_timestamp ASC")
    List<AudioSync> findSyncPointsByBookAndPage(@Param("bookId") Long bookId, @Param("page") Integer page);

    @Update("UPDATE audio_sync SET text = #{text}, is_verified = #{isVerified} WHERE id = #{id}")
    void updateAudioSync(AudioSync audioSync);

    @Delete("DELETE FROM audio_sync WHERE id = #{id}")
    void deleteAudioSync(@Param("id") Long id);
}
