package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.newspaper.ArticleReviewReply;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleReviewReplyMapper {

    /**
     * Insert new reply
     */
    @Insert("INSERT INTO article_review_replies (user_id, review_id, parent_reply_id, content, created_at, updated_at) " +
            "VALUES (#{userId}, #{reviewId}, #{parentReplyId}, #{content}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleReviewReply reply);

    /**
     * Update reply
     */
    @Update("UPDATE article_review_replies SET content = #{content}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void update(ArticleReviewReply reply);

    /**
     * Soft delete reply
     */
    @Update("UPDATE article_review_replies SET is_deleted = TRUE WHERE id = #{id}")
    void softDelete(@Param("id") Long id);

    /**
     * Find reply by ID
     */
    @Select("SELECT * FROM article_review_replies WHERE id = #{id} AND is_deleted = FALSE")
    ArticleReviewReply findById(@Param("id") Long id);

    /**
     * Find replies by review ID
     */
    @Select("SELECT * FROM article_review_replies " +
            "WHERE review_id = #{reviewId} AND is_deleted = FALSE " +
            "ORDER BY created_at ASC")
    List<ArticleReviewReply> findByReviewId(@Param("reviewId") Long reviewId);
}