package com.masasilam.app.mapper.chapter;

import com.masasilam.app.model.entity.ChapterRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChapterRatingMapper {
    void upsertRating(ChapterRating rating);
    ChapterRating findRating(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    Map<String, Object> getRatingSummary(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    List<Map<String, Object>> getRatingDistribution(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    void deleteRating(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber);
    List<ChapterRating> findUserBookRatings(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<Map<String, Object>> findAllUserRatings(@Param("userId") Long userId);
}