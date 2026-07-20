package com.naskah.app.mapper;

import com.naskah.app.model.dto.response.FilmRatingStatsResponse;
import com.naskah.app.model.film.FilmRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FilmRatingMapper {

    FilmRating findByUserAndFilm(@Param("userId") Long userId,
                                 @Param("filmId") Long filmId);

    FilmRatingStatsResponse getFilmRatingStats(@Param("filmId") Long filmId);

    void insert(FilmRating rating);

    void update(FilmRating rating);

    void delete(@Param("id") Long id);
}