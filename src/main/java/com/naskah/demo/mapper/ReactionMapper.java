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

    // Find main reactions by book (excluding replies)
    List<Reaction> findReactionsByBookId(@Param("bookId") Long bookId);
    List<Reaction> findReactionsByBookIdWithPagination(@Param("bookId") Long bookId,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    // Find main reactions with reply count
    List<Reaction> findMainReactionsByBookId(@Param("bookId") Long bookId,
                                             @Param("offset") Integer offset,
                                             @Param("limit") Integer limit);

    // Find existing main reaction by user and book (for updates)
    Reaction findReactionByUserAndBook(@Param("userId") Long userId,
                                       @Param("bookId") Long bookId);

    // Reply-related methods
    List<Reaction> findRepliesByParentId(@Param("parentId") Long parentId);
    List<Reaction> findRepliesByParentIdWithPagination(@Param("parentId") Long parentId,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);
    int countRepliesByParentId(@Param("parentId") Long parentId);

    // Stats and user-specific data
    ReactionStatsResponse getReactionStats(@Param("bookId") Long bookId);
    String getUserReactionType(@Param("userId") Long userId, @Param("bookId") Long bookId);
    Integer getUserRating(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // Count operations
    int countMainReactionsByBookId(@Param("bookId") Long bookId);
    int countReactionsByType(@Param("bookId") Long bookId, @Param("reactionType") String reactionType);

    // Delete operations
    void deleteReactionAndReplies(@Param("reactionId") Long reactionId);

    // Bulk operations
    List<Reaction> findReactionsByUserId(@Param("userId") Long userId,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);
}