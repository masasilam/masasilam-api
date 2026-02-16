package com.naskah.demo.service.film;

import com.naskah.demo.model.film.Film;
import com.naskah.demo.model.film.FilmDetail;
import java.util.List;

/**
 * FilmService - Service interface for film operations
 */
public interface FilmService {

    /**
     * Get complete film details by slug (SEO friendly)
     * @param slug Film slug
     * @return FilmDetail with all relationships
     */
    FilmDetail getFilmDetailBySlug(String slug);

    /**
     * Get complete film details by Wikidata QID
     * @param qid Wikidata QID (e.g., "Q623051")
     * @return FilmDetail with all relationships
     */
    FilmDetail getFilmDetailByQid(String qid);

    /**
     * Save or update film with all metadata
     * @param filmDetail Complete film data
     * @return Saved Film entity
     */
    Film saveFilm(FilmDetail filmDetail);

    /**
     * Get paginated list of all films
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @return List of Film entities
     */
    List<Film> getAllFilms(int page, int size);

    /**
     * Get total count of all films
     * @return Total film count
     */
    int getTotalFilms();

    /**
     * Search films by title
     * @param query Search query
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @return List of matching Film entities
     */
    List<Film> searchFilms(String query, int page, int size);

    /**
     * Get total count of search results
     * @param query Search query
     * @return Total matching films count
     */
    int getTotalSearchResults(String query);
}