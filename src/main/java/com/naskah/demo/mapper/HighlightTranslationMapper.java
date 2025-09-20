package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.HighlightTranslation;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HighlightTranslationMapper {

    @Select("SELECT * FROM highlight_translations WHERE highlight_id = #{highlightId} AND target_language = #{targetLanguage}")
    HighlightTranslation findByHighlightIdAndLanguage(@Param("highlightId") Long highlightId,
                                                      @Param("targetLanguage") String targetLanguage);

    @Insert("INSERT INTO highlight_translations (highlight_id, target_language, translated_text, created_at) " +
            "VALUES (#{highlightId}, #{targetLanguage}, #{translatedText}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertTranslation(HighlightTranslation translation);

    @Update("UPDATE highlight_translations SET translated_text = #{translatedText}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void updateTranslation(HighlightTranslation translation);

    @Delete("DELETE FROM highlight_translations WHERE highlight_id = #{highlightId}")
    void deleteByHighlightId(@Param("highlightId") Long highlightId);
}