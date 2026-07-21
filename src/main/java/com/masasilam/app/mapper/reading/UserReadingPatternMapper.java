package com.masasilam.app.mapper.reading;

import com.masasilam.app.model.entity.UserReadingPattern;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserReadingPatternMapper {
    UserReadingPattern findPattern(@Param("userId") Long userId, @Param("bookId") Long bookId);
    UserReadingPattern findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insert(UserReadingPattern pattern);
    void update(UserReadingPattern pattern);
}