package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ZineReadingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZineReadingProgressMapper {
    void insert(ZineReadingProgress progress);
    void update(ZineReadingProgress progress);

    ZineReadingProgress findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
    List<ZineReadingProgress> findAllByUser(Long userId);
}