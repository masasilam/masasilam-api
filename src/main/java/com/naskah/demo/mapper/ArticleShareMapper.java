package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.newspaper.ArticleShare;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ArticleShareMapper {

    /**
     * Insert article share
     */
    @Insert("INSERT INTO article_shares (article_id, user_id, platform, created_at) " +
            "VALUES (#{articleId}, #{userId}, #{platform}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleShare share);

    /**
     * Get share count by article
     */
    @Select("SELECT COUNT(*) FROM article_shares WHERE article_id = #{articleId}")
    int countByArticle(@Param("articleId") Long articleId);

    /**
     * Get share count by platform
     */
    @Select("SELECT COUNT(*) FROM article_shares " +
            "WHERE article_id = #{articleId} AND platform = #{platform}")
    int countByArticleAndPlatform(
            @Param("articleId") Long articleId,
            @Param("platform") String platform);
}