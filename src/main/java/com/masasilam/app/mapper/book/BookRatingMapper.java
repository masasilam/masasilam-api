package com.masasilam.app.mapper.book;

import com.masasilam.app.model.dto.response.BookRatingStatsResponse;
import com.masasilam.app.model.entity.BookRating;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BookRatingMapper {
    void insert(BookRating bookRating);
    void update(BookRating bookRating);
    void delete(@Param("id") Long id);
    BookRating findById(@Param("id") Long id);
    BookRating findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    BookRatingStatsResponse getBookRatingStats(@Param("bookId") Long bookId);
}