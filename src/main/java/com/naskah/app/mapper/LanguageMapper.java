package com.naskah.app.mapper;

import com.naskah.app.model.entity.Language;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LanguageMapper {
    Language findLanguageByName(@Param("name") String name);
    String findLanguageNameById(@Param("id") Integer id);

    void insertLanguage(Language language);
}
