package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookReviewReplyMapper {

    void insert(BookReviewReply bookReviewReply);

    void update(BookReviewReply bookReviewReply);

    void softDelete(@Param("id") Long id);

    void delete(@Param("id") Long id);

    BookReviewReply findById(@Param("id") Long id);

    List<BookReviewReply> findByReviewId(@Param("reviewId") Long reviewId);

    List<BookReviewReply> findByParentReplyId(@Param("parentReplyId") Long parentReplyId);

    int countByReviewId(@Param("reviewId") Long reviewId);
}