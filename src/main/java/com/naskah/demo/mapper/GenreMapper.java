package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.GenreResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GenreMapper {
    List<GenreResponse> findGenresByBookId(@Param("bookId") Long bookId);
}
