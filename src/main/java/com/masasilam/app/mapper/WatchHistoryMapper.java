package com.masasilam.app.mapper;

import com.masasilam.app.model.film.WatchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WatchHistoryMapper {

    /** Cari berdasarkan user ID (untuk user yang sudah login) */
    WatchHistory findByUserAndFilm(@Param("userId") Long userId,
                                   @Param("filmId") Long filmId);

    /** Cari berdasarkan viewer hash (untuk guest) */
    WatchHistory findByHashAndFilm(@Param("viewerHash") String viewerHash,
                                   @Param("filmId") Long filmId);

    /** Daftar film yang pernah ditonton user (untuk fitur "Continue Watching") */
    List<WatchHistory> findByUser(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    void insert(WatchHistory history);

    void update(WatchHistory history);
}