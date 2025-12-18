package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Genre;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface GenreMapper {

    // Find genre by name (exact match)
    Genre findByName(@Param("name") String name);

    // Find genre by slug
    Genre findBySlug(@Param("slug") String slug);

    // Find genre by ID
    Genre findById(@Param("id") Long id);

    // Get all genres
    List<Genre> findAll();

    // Insert new genre
    void insertGenre(Genre genre);

    // Update genre
    void updateGenre(Genre genre);

    // Delete genre
    void deleteGenre(@Param("id") Long id);

    // Count genres by name (for duplicate check)
    int countByName(@Param("name") String name);

    @Select("SELECT g.*, COUNT(bg.book_id) AS book_count " +
            "FROM genres g " +
            "LEFT JOIN book_genres bg ON g.id = bg.genre_id " +
            "GROUP BY g.id " +
            "ORDER BY g.name ASC")
    List<Genre> findAllWithBookCount();

    @Select("SELECT g.name, COUNT(DISTINCT rs.book_id) as book_count " +
            "FROM genres g " +
            "JOIN book_genres bg ON g.id = bg.genre_id " +
            "JOIN reading_sessions rs ON bg.book_id = rs.book_id " +
            "WHERE rs.user_id = #{userId} " +
            "GROUP BY g.name " +
            "ORDER BY book_count DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "name", column = "name"),
            @Result(property = "bookCount", column = "book_count")
    })
    List<Map<String, Object>> getUserFavoriteGenres(
            @Param("userId") Long userId,
            @Param("limit") int limit);
}