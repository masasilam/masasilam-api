package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.ReadingProgress;
import com.masasilam.app.model.entity.ReadingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadingMapper {

    ReadingProgress findReadingProgressByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insertReadingProgress(ReadingProgress readingProgress);
    void updateReadingProgress(ReadingProgress readingProgress);
    void insertReadingSession(ReadingSession readingSession);
}
