package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Reaction;
import com.naskah.demo.model.dto.response.ReactionResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReactionMapper {

    @Insert("INSERT INTO reactions (user_id, book_id, reaction_type, page, position, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{reactionType}, #{page}, #{position}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertReaction(Reaction reaction);

    @Select("SELECT * FROM reactions WHERE user_id = #{userId} AND book_id = #{bookId} AND page = #{page} AND position = #{position}")
    Reaction findReactionByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId,
                                       @Param("page") Integer page, @Param("position") String position);

    @Update("UPDATE reactions SET reaction_type = #{reactionType}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateReaction(Reaction reaction);

    @Select("SELECT reaction_type, COUNT(*) as count FROM reactions WHERE book_id = #{bookId} " +
            "AND (#{page} IS NULL OR page = #{page}) " +
            "AND (#{position} IS NULL OR position = #{position}) " +
            "GROUP BY reaction_type")
    List<ReactionResponse.ReactionStats> getReactionStats(@Param("bookId") Long bookId,
                                                          @Param("page") Integer page,
                                                          @Param("position") String position);

    @Delete("DELETE FROM reactions WHERE id = #{id}")
    void deleteReaction(@Param("id") Long id);
}