package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.entity.newspaper.SavedArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SavedArticleMapper {
    void insert(SavedArticle saved);
    void delete(@Param("id") Long id);
    SavedArticle findByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
}