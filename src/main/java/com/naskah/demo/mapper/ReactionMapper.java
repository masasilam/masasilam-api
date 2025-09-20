package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Reaction;
import com.naskah.demo.model.dto.response.ReactionStatsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReactionMapper {

    // Basic CRUD operations
    void insertReaction(Reaction reaction);
    void updateReaction(Reaction reaction);
    void deleteReaction(@Param("reactionId") Long reactionId);
    Reaction findReactionById(@Param("reactionId") Long reactionId);

    // Find reactions by book
    List<Reaction> findReactionsByBookId(@Param("bookId") Long bookId);
    List<Reaction> findReactionsByBookIdWithPagination(@Param("bookId") Long bookId,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    // Find existing reaction by user and book (for updates)
    Reaction findReactionByUserAndBook(@Param("userId") Long userId,
                                       @Param("bookId") Long bookId,
                                       @Param("page") Integer page,
                                       @Param("position") String position);

    // Reply-related methods
    List<Reaction> findRepliesByParentId(@Param("parentId") Long parentId);
    int countRepliesByParentId(@Param("parentId") Long parentId);

    // Stats and user-specific data
    ReactionStatsResponse getReactionStats(@Param("bookId") Long bookId);
    String getUserReactionType(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // Delete operations
    void deleteReactionAndReplies(@Param("reactionId") Long reactionId);
}