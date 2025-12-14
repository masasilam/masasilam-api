package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}