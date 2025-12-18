package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterRating;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChapterRatingMapper {

    @Insert("INSERT INTO chapter_ratings (user_id, book_id, chapter_number, rating, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{chapterNumber}, #{rating}, #{createdAt}, #{updatedAt}) " +
            "ON CONFLICT (user_id, book_id, chapter_number) " +
            "DO UPDATE SET rating = #{rating}, updated_at = #{updatedAt}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void upsertRating(ChapterRating rating);

    @Select("SELECT * FROM chapter_ratings " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} AND chapter_number = #{chapterNumber}")
    ChapterRating findRating(@Param("userId") Long userId,
                             @Param("bookId") Long bookId,
                             @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT AVG(rating) as average_rating, COUNT(*) as total_ratings " +
            "FROM chapter_ratings " +
            "WHERE book_id = #{bookId} AND chapter_number = #{chapterNumber}")
    Map<String, Object> getRatingSummary(@Param("bookId") Long bookId,
                                         @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT rating, COUNT(*) as count " +
            "FROM chapter_ratings " +
            "WHERE book_id = #{bookId} AND chapter_number = #{chapterNumber} " +
            "GROUP BY rating")
    List<Map<String, Object>> getRatingDistribution(@Param("bookId") Long bookId,
                                                    @Param("chapterNumber") Integer chapterNumber);

    @Delete("DELETE FROM chapter_ratings " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} AND chapter_number = #{chapterNumber}")
    void deleteRating(@Param("userId") Long userId,
                      @Param("bookId") Long bookId,
                      @Param("chapterNumber") Integer chapterNumber);

    @Select("SELECT * FROM chapter_ratings " +
            "WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "ORDER BY chapter_number")
    List<ChapterRating> findUserBookRatings(@Param("userId") Long userId,
                                            @Param("bookId") Long bookId);
}