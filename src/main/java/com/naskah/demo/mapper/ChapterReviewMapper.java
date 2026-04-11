package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChapterReviewMapper {

    // ── Existing methods (tidak berubah) ──────────────────────────────────────

    List<ChapterReview> findReviewsByChapter(
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber,
            @Param("offset") int offset,
            @Param("limit") int limit);

    void insertChapterReview(ChapterReview review);

    ChapterReview findById(@Param("reviewId") Long reviewId);

    void likeReview(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);

    void incrementLikeCount(@Param("reviewId") Long reviewId);

    void unlikeReview(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);

    void decrementLikeCount(@Param("reviewId") Long reviewId);

    boolean isReviewLikedByUser(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);

    List<ChapterReview> findReplies(@Param("reviewId") Long reviewId);

    // ── New methods — dipanggil DashboardServiceImpl ──────────────────────────

    /**
     * Ambil semua review yang ditulis user, dengan paginasi.
     * Dipanggil DashboardServiceImpl.getUserReviews()
     *
     * SELECT * FROM chapter_reviews
     * WHERE user_id = #{userId}
     * ORDER BY created_at DESC
     * LIMIT #{limit} OFFSET #{offset}
     */
    List<ChapterReview> findByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * Hitung total review yang ditulis user.
     * Dipanggil DashboardServiceImpl.buildAnnotationsSummary()
     *
     * SELECT COUNT(*) FROM chapter_reviews WHERE user_id = #{userId}
     */
    @Select("SELECT COUNT(*) FROM chapter_reviews WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);
}