package com.naskah.demo.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TranslationCacheMapper {

    @Insert("INSERT INTO translation_cache (book_id, page, source_language, target_language, original_text, translated_text, created_at) " +
            "VALUES (#{bookId}, #{page}, #{sourceLanguage}, #{targetLanguage}, #{originalText}, #{translatedText}, #{createdAt})")
    void insertTranslation(@Param("bookId") Long bookId, @Param("page") Integer page,
                           @Param("sourceLanguage") String sourceLanguage, @Param("targetLanguage") String targetLanguage,
                           @Param("originalText") String originalText, @Param("translatedText") String translatedText,
                           @Param("createdAt") java.time.LocalDateTime createdAt);

    @Select("SELECT translated_text FROM translation_cache " +
            "WHERE book_id = #{bookId} AND page = #{page} AND target_language = #{targetLanguage} " +
            "ORDER BY created_at DESC LIMIT 1")
    String findCachedTranslation(@Param("bookId") Long bookId, @Param("page") Integer page, @Param("targetLanguage") String targetLanguage);

    @Delete("DELETE FROM translation_cache WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    void cleanupOldTranslations();
}