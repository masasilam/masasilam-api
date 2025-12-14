package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterReview;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChapterReviewMapper {
    List<ChapterReview> findReviewsByChapter(Long id, Integer chapterNumber, int offset, int limit);

    void insertChapterReview(ChapterReview review);

    ChapterReview findById(Long reviewId);

    void likeReview(Long reviewId, Long id);

    void incrementLikeCount(Long reviewId);

    void unlikeReview(Long reviewId, Long id);

    void decrementLikeCount(Long reviewId);

    boolean isReviewLikedByUser(Long id, Long currentUserId);

    List<ChapterReview> findReplies(Long id);
}
