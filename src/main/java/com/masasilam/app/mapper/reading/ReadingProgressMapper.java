package com.masasilam.app.mapper.reading;

import com.masasilam.app.model.entity.ReadingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingProgressMapper {
    ReadingProgress findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<ReadingProgress> findAllByUser(@Param("userId") Long userId);
    void updateReadingProgress(ReadingProgress overallProgress);
    void insert(ReadingProgress progress);
    void update(ReadingProgress progress);
    void updateTimeOnly(ReadingProgress progress);
    ReadingProgress findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}