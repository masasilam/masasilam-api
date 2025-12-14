package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Genre;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}