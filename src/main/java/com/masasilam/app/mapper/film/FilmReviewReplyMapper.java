package com.masasilam.app.mapper.film;

import com.masasilam.app.model.entity.film.FilmReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmReviewReplyMapper {
    FilmReviewReply findById(@Param("id") Long id);
    List<FilmReviewReply> findByReviewId(@Param("reviewId") Long reviewId);
    void insert(FilmReviewReply reply);
    void update(FilmReviewReply reply);
    void softDelete(@Param("id") Long id);
}