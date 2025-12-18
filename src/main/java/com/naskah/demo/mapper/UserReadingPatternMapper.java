package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.UserReadingPattern;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserReadingPatternMapper {

    @Insert("INSERT INTO user_reading_patterns (" +
            "user_id, book_id, preferred_reading_hour, preferred_day_of_week, " +
            "average_session_duration_minutes, skip_rate, reread_rate, " +
            "completion_speed_chapters_per_day, annotation_frequency, average_reading_speed_wpm, " +
            "last_calculated_at, created_at) " +
            "VALUES (#{userId}, #{bookId}, #{preferredReadingHour}, #{preferredDayOfWeek}, " +
            "#{averageSessionDurationMinutes}, #{skipRate}, #{rereadRate}, " +
            "#{completionSpeedChaptersPerDay}, #{annotationFrequency}, #{averageReadingSpeedWpm}, " +
            "#{lastCalculatedAt}, #{createdAt}) " +
            "ON CONFLICT (user_id, book_id) " +
            "DO UPDATE SET " +
            "preferred_reading_hour = #{preferredReadingHour}, " +
            "preferred_day_of_week = #{preferredDayOfWeek}, " +
            "average_session_duration_minutes = #{averageSessionDurationMinutes}, " +
            "skip_rate = #{skipRate}, reread_rate = #{rereadRate}, " +
            "completion_speed_chapters_per_day = #{completionSpeedChaptersPerDay}, " +
            "annotation_frequency = #{annotationFrequency}, " +
            "average_reading_speed_wpm = #{averageReadingSpeedWpm}, " +
            "last_calculated_at = #{lastCalculatedAt}")
    void upsertPattern(UserReadingPattern pattern);

    @Select("SELECT * FROM user_reading_patterns " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    UserReadingPattern findPattern(@Param("userId") Long userId,
                                   @Param("bookId") Long bookId);

    @Select("SELECT * FROM user_reading_patterns " +
            "WHERE user_id = #{userId} " +
            "ORDER BY last_calculated_at DESC")
    List<UserReadingPattern> findUserPatterns(@Param("userId") Long userId);

    @Select("SELECT * FROM user_reading_patterns " +
            "WHERE user_id = #{userId} AND book_id = #{bookId}")
    UserReadingPattern findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId);

    @Select("SELECT * FROM user_reading_patterns WHERE user_id = #{userId}")
    List<UserReadingPattern> findAllUserPatterns(@Param("userId") Long userId);

    @Insert("INSERT INTO user_reading_patterns " +
            "(user_id, book_id, total_reading_time_minutes, average_session_duration_minutes, " +
            "average_reading_speed_wpm, completion_speed_chapters_per_day, " +
            "preferred_reading_hour, sessions_count, created_at, updated_at) " +
            "VALUES (#{userId}, #{bookId}, #{totalReadingTimeMinutes}, " +
            "#{averageSessionDurationMinutes}, #{averageReadingSpeedWpm}, " +
            "#{completionSpeedChaptersPerDay}, #{preferredReadingHour}, " +
            "#{sessionsCount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserReadingPattern pattern);

    @Update("UPDATE user_reading_patterns SET " +
            "total_reading_time_minutes = #{totalReadingTimeMinutes}, " +
            "average_session_duration_minutes = #{averageSessionDurationMinutes}, " +
            "average_reading_speed_wpm = #{averageReadingSpeedWpm}, " +
            "completion_speed_chapters_per_day = #{completionSpeedChaptersPerDay}, " +
            "preferred_reading_hour = #{preferredReadingHour}, " +
            "sessions_count = #{sessionsCount}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(UserReadingPattern pattern);
}
