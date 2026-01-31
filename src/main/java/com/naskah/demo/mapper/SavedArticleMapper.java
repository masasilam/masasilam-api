package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.newspaper.SavedArticle;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SavedArticleMapper {

    /**
     * Insert saved article
     */
    @Insert("INSERT INTO saved_articles (user_id, article_id, collection_name, notes, created_at) " +
            "VALUES (#{userId}, #{articleId}, #{collectionName}, #{notes}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SavedArticle saved);

    /**
     * Delete saved article
     */
    @Delete("DELETE FROM saved_articles WHERE id = #{id}")
    void delete(@Param("id") Long id);

    /**
     * Find saved article by user and article
     */
    @Select("SELECT * FROM saved_articles WHERE user_id = #{userId} AND article_id = #{articleId}")
    SavedArticle findByUserAndArticle(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId);

    /**
     * Find all saved articles by user
     */
    @Select("SELECT * FROM saved_articles WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<SavedArticle> findByUserId(@Param("userId") Long userId);
}
