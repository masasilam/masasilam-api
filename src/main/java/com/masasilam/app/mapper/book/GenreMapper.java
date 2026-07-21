package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.Genre;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GenreMapper {
    Genre findByName(@Param("name") String name);
    Genre findBySlug(@Param("slug") String slug);
    Genre findById(@Param("id") Long id);
    List<Genre> findAll();
    void insertGenre(Genre genre);
    void updateGenre(Genre genre);
    void deleteGenre(@Param("id") Long id);
    int countByName(@Param("name") String name);
    List<Genre> findAllWithBookCount();
}