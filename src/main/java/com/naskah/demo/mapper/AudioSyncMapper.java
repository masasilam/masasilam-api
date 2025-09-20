package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.AudioSync;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AudioSyncMapper {

    @Select("SELECT * FROM audio_syncs WHERE book_id = #{bookId} AND page = #{page} AND text_position = #{textPosition}")
    AudioSync findByBookPageAndPosition(@Param("bookId") Long bookId,
                                        @Param("page") Integer page,
                                        @Param("textPosition") Integer textPosition);

    @Select("SELECT * FROM audio_syncs WHERE book_id = #{bookId} AND page = #{page} ORDER BY text_position ASC")
    List<AudioSync> findByBookIdAndPage(@Param("bookId") Long bookId, @Param("page") Integer page);

    @Insert("INSERT INTO audio_syncs (book_id, page, text_position, audio_timestamp, created_at) " +
            "VALUES (#{bookId}, #{page}, #{textPosition}, #{audioTimestamp}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertAudioSync(AudioSync audioSync);

    @Update("UPDATE audio_syncs SET audio_timestamp = #{audioTimestamp}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void updateAudioSync(AudioSync audioSync);

    @Delete("DELETE FROM audio_syncs WHERE book_id = #{bookId}")
    void deleteByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM audio_syncs WHERE book_id = #{bookId} AND page = #{page}")
    void deleteByBookIdAndPage(@Param("bookId") Long bookId, @Param("page") Integer page);
}
