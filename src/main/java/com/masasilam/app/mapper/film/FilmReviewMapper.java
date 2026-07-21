package com.masasilam.app.mapper.film;

import com.masasilam.app.model.film.FilmReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmReviewMapper {
    FilmReview findById(@Param("id") Long id);
    FilmReview findByUserAndFilm(@Param("userId") Long userId, @Param("filmId") Long filmId);
    List<FilmReview> findByFilmWithPagination(@Param("filmId") Long filmId, @Param("offset") int offset, @Param("limit") int limit, @Param("sortBy") String sortBy);
    int countByFilm(@Param("filmId") Long filmId);
    void insert(FilmReview review);
    void update(FilmReview review);
    void softDelete(@Param("id") Long id);
    void incrementReplyCount(@Param("id") Long id);
    void decrementReplyCount(@Param("id") Long id);
    void updateHelpfulCounts(@Param("reviewId") Long reviewId);
}