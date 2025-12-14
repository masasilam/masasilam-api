package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.BookRatingStatsResponse;
import com.naskah.demo.model.entity.BookRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookRatingMapper {

    void insert(BookRating bookRating);

    void update(BookRating bookRating);

    void delete(@Param("id") Long id);

    BookRating findById(@Param("id") Long id);

    BookRating findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    BookRatingStatsResponse getBookRatingStats(@Param("bookId") Long bookId);
}