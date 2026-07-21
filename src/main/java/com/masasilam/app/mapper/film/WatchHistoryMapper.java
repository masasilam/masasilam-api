package com.masasilam.app.mapper.film;

import com.masasilam.app.model.film.WatchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WatchHistoryMapper {
    WatchHistory findByUserAndFilm(@Param("userId") Long userId, @Param("filmId") Long filmId);
    WatchHistory findByHashAndFilm(@Param("viewerHash") String viewerHash, @Param("filmId") Long filmId);
    List<WatchHistory> findByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    void insert(WatchHistory history);
    void update(WatchHistory history);
}