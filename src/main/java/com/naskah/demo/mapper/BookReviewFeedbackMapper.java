package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookReviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookReviewFeedbackMapper {

    void insert(BookReviewFeedback bookReviewFeedback);

    void update(BookReviewFeedback bookReviewFeedback);

    void delete(@Param("id") Long id);

    BookReviewFeedback findById(@Param("id") Long id);

    BookReviewFeedback findByUserAndReview(@Param("userId") Long userId, @Param("reviewId") Long reviewId);

    int countHelpfulByReview(@Param("reviewId") Long reviewId);

    int countNotHelpfulByReview(@Param("reviewId") Long reviewId);
}