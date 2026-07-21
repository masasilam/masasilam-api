package com.masasilam.app.mapper.newspaper;

import com.masasilam.app.model.entity.newspaper.ArticleReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleReviewReplyMapper {
    void insert(ArticleReviewReply reply);
    void update(ArticleReviewReply reply);
    void softDelete(@Param("id") Long id);
    ArticleReviewReply findById(@Param("id") Long id);
    List<ArticleReviewReply> findByReviewId(@Param("reviewId") Long reviewId);
}