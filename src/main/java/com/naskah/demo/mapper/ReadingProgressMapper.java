package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingProgress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReadingProgressMapper {
    ReadingProgress findByUserAndBook(Long userId, Long bookId);

    void updateReadingProgress(ReadingProgress overallProgress);
}
