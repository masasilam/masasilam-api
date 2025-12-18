package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookReview;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookReviewMapper {

    void insert(BookReview bookReview);

    void update(BookReview bookReview);

    void softDelete(@Param("id") Long id);

    void delete(@Param("id") Long id);

    BookReview findById(@Param("id") Long id);

    BookReview findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    List<BookReview> findByBookWithPagination(
            @Param("bookId") Long bookId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy);

    int countByBook(@Param("bookId") Long bookId);

    @Select("SELECT * FROM book_reviews WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<BookReview> findByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("SELECT * FROM book_reviews WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<BookReview> findAllByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM book_reviews WHERE user_id = #{userId} " +
            "AND created_at >= #{since} " +
            "ORDER BY created_at DESC")
    List<BookReview> findByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM book_reviews WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM book_reviews WHERE book_id = #{bookId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<BookReview> findByBook(
            @Param("bookId") Long bookId,
            @Param("offset") int offset,
            @Param("limit") int limit);
}