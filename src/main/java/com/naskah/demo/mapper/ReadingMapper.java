package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingProgress;
import com.naskah.demo.model.entity.ReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadingMapper {

    ReadingProgress findReadingProgressByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insertReadingProgress(ReadingProgress readingProgress);
    void updateReadingProgress(ReadingProgress readingProgress);
    void insertReadingSession(ReadingSession readingSession);
}
