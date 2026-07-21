package com.masasilam.app.mapper.chapter;

import com.masasilam.app.model.entity.ChapterReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterReviewMapper {
    List<ChapterReview> findReviewsByChapter(@Param("bookId") Long bookId, @Param("chapterNumber") Integer chapterNumber, @Param("offset") int offset, @Param("limit") int limit);
    void insertChapterReview(ChapterReview review);
    ChapterReview findById(@Param("reviewId") Long reviewId);
    void likeReview(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
    void incrementLikeCount(@Param("reviewId") Long reviewId);
    void unlikeReview(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
    void decrementLikeCount(@Param("reviewId") Long reviewId);
    boolean isReviewLikedByUser(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
    List<ChapterReview> findReplies(@Param("reviewId") Long reviewId);
    List<ChapterReview> findByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    Integer countByUser(@Param("userId") Long userId);
}