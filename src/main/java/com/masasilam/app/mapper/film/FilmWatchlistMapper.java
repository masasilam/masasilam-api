package com.masasilam.app.mapper.film;

import com.masasilam.app.model.dto.response.FilmWatchlistResponse;
import com.masasilam.app.model.entity.film.FilmWatchlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmWatchlistMapper {
    FilmWatchlist findByUserAndFilm(@Param("userId") Long userId, @Param("filmId") Long filmId);
    List<FilmWatchlistResponse> findByUserWithFilmInfo(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    int countByUser(@Param("userId") Long userId);
    void insert(FilmWatchlist watchlist);
    void delete(@Param("id") Long id);
}