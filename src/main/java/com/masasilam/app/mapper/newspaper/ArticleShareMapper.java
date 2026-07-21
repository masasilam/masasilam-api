package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.entity.newspaper.ArticleShare;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleShareMapper {
    void insert(ArticleShare share);
}