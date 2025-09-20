package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ReactionStatsResponse;
import com.naskah.demo.model.entity.Reaction;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReactionMapper {

    @Select({"<script>",
            "SELECT * FROM reactions WHERE user_id = #{userId} AND book_id = #{bookId}",
            "<if test='page != null'>",
            " AND page = #{page}",
            "</if>",
            "<if test='position != null'>",
            " AND position = #{position}",
            "</if>",
            "</script>"})
    Reaction findReactionByUserAndBook(@Param("userId") Long userId,
                                       @Param("bookId") Long bookId,
                                       @Param("page") Integer page,
                                       @Param("position") String position);

    @Select("SELECT * FROM reactions WHERE id = #{id}")
    Reaction findReactionById(@Param("id") Long id);

    @Insert("INSERT INTO reactions (user_id, book_id, reaction_type, rating, comment, page, position, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{reactionType}, #{rating}, #{comment}, #{page}, #{position}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertReaction(Reaction reaction);

    @Update("UPDATE reactions SET reaction_type = #{reactionType}, rating = #{rating}, " +
            "comment = #{comment}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateReaction(Reaction reaction);

    @Delete("DELETE FROM reactions WHERE id = #{id}")
    void deleteReaction(@Param("id") Long id);

    @Select("SELECT r.*, u.username FROM reactions r " +
            "JOIN users u ON r.user_id = u.id " +
            "WHERE r.book_id = #{bookId} " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Reaction> findReactionsByBookIdWithPaging(@Param("bookId") Long bookId,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    @Select("SELECT r.* FROM reactions r WHERE r.book_id = #{bookId} ORDER BY r.created_at DESC")
    List<Reaction> findReactionsByBookId(@Param("bookId") Long bookId);

    @Select("SELECT " +
            "SUM(CASE WHEN reaction_type = 'rating' THEN 1 ELSE 0 END) as totalRatings, " +
            "SUM(CASE WHEN reaction_type = 'angry' THEN 1 ELSE 0 END) as totalAngry, " +
            "SUM(CASE WHEN reaction_type = 'like' THEN 1 ELSE 0 END) as totalLikes, " +
            "SUM(CASE WHEN reaction_type = 'love' THEN 1 ELSE 0 END) as totalLoves, " +
            "AVG(CASE WHEN reaction_type = 'rating' AND rating IS NOT NULL THEN rating END) as averageRating " +
            "FROM reactions WHERE book_id = #{bookId}")
    ReactionStatsResponse getReactionStats(@Param("bookId") Long bookId);

    @Select("SELECT reaction_type FROM reactions WHERE user_id = #{userId} AND book_id = #{bookId} LIMIT 1")
    String getUserReactionType(@Param("userId") Long userId, @Param("bookId") Long bookId);
}