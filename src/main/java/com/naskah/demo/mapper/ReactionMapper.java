package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Reaction;
import com.naskah.demo.model.dto.response.ReactionStatsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReactionMapper {
    void insertReaction(Reaction reaction);
    void updateReaction(Reaction reaction);
    Reaction findReactionById(@Param("reactionId") Long reactionId);
    List<Reaction> findReactionsByBookIdWithPagination(@Param("bookId") Long bookId, @Param("offset") int offset, @Param("limit") int limit);
    int countRepliesByParentId(@Param("parentId") Long parentId);
    ReactionStatsResponse getReactionStats(@Param("bookId") Long bookId);
    String getUserReactionType(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void deleteReactionAndReplies(@Param("reactionId") Long reactionId);
    Reaction findReviewByUserAndBook(@Param("bookId") Long bookId, @Param("userId") Long userId);
    Reaction findFeedbackByUserAndReview(@Param("userId") Long userId, @Param("parentId") Long parentId);
    Reaction findRatingByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}