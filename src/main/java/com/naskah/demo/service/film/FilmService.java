package com.naskah.demo.service.film;

import com.naskah.demo.model.film.Film;
import com.naskah.demo.model.film.FilmDetail;
import java.util.List;

public interface FilmService {
    FilmDetail getFilmDetailBySlug(String slug);
    FilmDetail getFilmDetailByQid(String qid);
    Film saveFilm(FilmDetail filmDetail);
    List<Film> getAllFilms(int page, int size);
    int getTotalFilms();
    List<Film> searchFilms(String query, int page, int size);
    int getTotalSearchResults(String query);
}