package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingProgressMapper {

    ReadingProgress findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId")  Long bookId);

    void updateReadingProgress(ReadingProgress overallProgress);

    List<ReadingProgress> findAllByUser(@Param("userId") Long userId);
}