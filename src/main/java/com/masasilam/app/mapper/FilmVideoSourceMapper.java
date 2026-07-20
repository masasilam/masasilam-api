package com.masasilam.app.mapper;

import com.masasilam.app.model.film.FilmVideoSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmVideoSourceMapper {

    List<FilmVideoSource> findByFilmId(@Param("filmId") Long filmId);
    List<FilmVideoSource> findActiveByFilmId(@Param("filmId") Long filmId);
    FilmVideoSource findById(@Param("id") Long id);
    void insert(FilmVideoSource source);
    void update(FilmVideoSource source);
    void delete(@Param("id") Long id);
    void deleteByFilmId(@Param("filmId") Long filmId);
    void deactivate(@Param("id") Long id);
}