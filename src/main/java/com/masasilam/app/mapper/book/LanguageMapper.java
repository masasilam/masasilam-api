package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.Language;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LanguageMapper {
    Language findLanguageByName(@Param("name") String name);
    String findLanguageNameById(@Param("id") Integer id);
}
