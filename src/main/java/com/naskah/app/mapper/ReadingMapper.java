package com.naskah.app.mapper;

import com.naskah.app.model.entity.ReadingProgress;
import com.naskah.app.model.entity.ReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadingMapper {

    ReadingProgress findReadingProgressByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insertReadingProgress(ReadingProgress readingProgress);
    void updateReadingProgress(ReadingProgress readingProgress);
    void insertReadingSession(ReadingSession readingSession);
}
