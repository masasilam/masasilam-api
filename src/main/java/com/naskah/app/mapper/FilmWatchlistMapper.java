package com.naskah.app.mapper;

import com.naskah.app.model.dto.response.FilmWatchlistResponse;
import com.naskah.app.model.film.FilmWatchlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmWatchlistMapper {

    FilmWatchlist findByUserAndFilm(@Param("userId") Long userId,
                                    @Param("filmId") Long filmId);

    /**
     * Query dengan JOIN ke tabel film untuk mengambil detail film sekaligus.
     * Menghindari N+1 query saat membangun response watchlist.
     */
    List<FilmWatchlistResponse> findByUserWithFilmInfo(@Param("userId") Long userId,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    int countByUser(@Param("userId") Long userId);

    void insert(FilmWatchlist watchlist);

    void delete(@Param("id") Long id);
}