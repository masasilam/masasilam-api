package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.BookRatingStatsResponse;
import com.naskah.demo.model.entity.BookRating;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookRatingMapper {

    void insert(BookRating bookRating);

    void update(BookRating bookRating);

    void delete(@Param("id") Long id);

    BookRating findById(@Param("id") Long id);

    BookRating findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    BookRatingStatsResponse getBookRatingStats(@Param("bookId") Long bookId);

    @Select("SELECT * FROM book_ratings WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<BookRating> findByUser(@Param("userId") Long userId);

    @Select("SELECT AVG(rating) FROM book_ratings WHERE user_id = #{userId}")
    Double getUserAverageRating(@Param("userId") Long userId);

    @Select("SELECT AVG(rating) FROM book_ratings WHERE book_id = #{bookId}")
    Double getBookAverageRating(@Param("bookId") Long bookId);

    @Select("SELECT COUNT(*) FROM book_ratings WHERE book_id = #{bookId}")
    Integer getBookRatingCount(@Param("bookId") Long bookId);

    @Select("SELECT * FROM book_ratings " +
            "WHERE user_id = #{userId} AND created_at >= #{since}")
    List<BookRating> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);
}