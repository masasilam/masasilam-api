package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ChapterListeningProgress;
import com.naskah.demo.model.entity.ChapterProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChapterProgressMapper {

    ChapterProgress findProgress(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );

    ChapterListeningProgress findListeningProgress(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterNumber") Integer chapterNumber
    );

    void insertListeningProgress(ChapterListeningProgress progress);

    void updateListeningProgress(ChapterListeningProgress progress);

    List<ChapterProgress> findAllByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    void updateProgress(ChapterProgress progress);

    void insertProgress(ChapterProgress progress);

    @Select("SELECT * FROM chapter_progress WHERE user_id = #{userId}")
    List<ChapterProgress> findAllByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT book_id) FROM chapter_progress " +
            "WHERE user_id = #{userId} AND is_completed = true " +
            "AND book_id IN (" +
            "  SELECT book_id FROM books " +
            "  WHERE total_pages = (" +
            "    SELECT COUNT(*) FROM chapter_progress cp2 " +
            "    WHERE cp2.user_id = #{userId} AND cp2.book_id = chapter_progress.book_id " +
            "    AND cp2.is_completed = true" +
            "  )" +
            ")")
    Integer countCompletedBooks(@Param("userId") Long userId);

    @Select("SELECT * FROM chapter_progress " +
            "WHERE user_id = #{userId} AND is_completed = true " +
            "AND last_read_at >= #{start} AND last_read_at < #{end}")
    List<ChapterProgress> findCompletedBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT book_id) FROM chapter_progress " +
            "WHERE user_id = #{userId} AND is_completed = true " +
            "AND last_read_at >= #{start} AND last_read_at < #{end}")
    Integer countBooksCompletedBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}