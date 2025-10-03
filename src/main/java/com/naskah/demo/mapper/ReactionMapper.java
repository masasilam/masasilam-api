package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Reaction;
import com.naskah.demo.model.dto.response.ReactionStatsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReactionMapper {

    // ============ BASIC CRUD ============
    void insertReaction(Reaction reaction);
    void updateReaction(Reaction reaction);
    Reaction findReactionById(@Param("reactionId") Long reactionId);
    void deleteReaction(@Param("reactionId") Long reactionId);
    void deleteReactionAndReplies(@Param("reactionId") Long reactionId);

    // ============ RATING QUERIES ============
    Reaction findRatingByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // ============ REVIEW QUERIES ============
    List<Reaction> findReviewsByBookIdWithPagination(
            @Param("bookId") Long bookId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    Reaction findReviewByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // ============ REPLY QUERIES ============
    List<Reaction> findRepliesByParentId(@Param("parentId") Long parentId);
    int countRepliesByParentId(@Param("parentId") Long parentId);

    // ============ FEEDBACK QUERIES ============
    Reaction findFeedbackByUserAndReview(@Param("userId") Long userId, @Param("parentId") Long parentId);
    int countHelpfulByReviewId(@Param("reviewId") Long reviewId);
    int countNotHelpfulByReviewId(@Param("reviewId") Long reviewId);

    // ============ STATISTICS ============
    ReactionStatsResponse getReactionStats(@Param("bookId") Long bookId);
    String getUserReactionType(@Param("userId") Long userId, @Param("bookId") Long bookId);
}