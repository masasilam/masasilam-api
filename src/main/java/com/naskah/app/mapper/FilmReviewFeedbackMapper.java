package com.naskah.app.mapper;

import com.naskah.app.model.film.FilmReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FilmReviewFeedbackMapper {

    FilmReviewFeedback findByUserAndReview(@Param("userId") Long userId,
                                           @Param("reviewId") Long reviewId);

    void insert(FilmReviewFeedback feedback);

    void update(FilmReviewFeedback feedback);

    void delete(@Param("id") Long id);
}