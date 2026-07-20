package com.naskah.app.mapper;

import com.naskah.app.model.film.FilmReviewReply;
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