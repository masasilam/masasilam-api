package com.masasilam.app.service.film;

import com.masasilam.app.model.dto.request.AddFilmRequest;
import com.masasilam.app.model.dto.request.UpdateFilmRequest;
import com.masasilam.app.model.film.Film;
import com.masasilam.app.model.film.FilmDetail;

import java.util.List;

public interface FilmService {
    FilmDetail getFilmDetailBySlug(String slug);
    Film addFilm(AddFilmRequest request);
    Film updateFilm(String slug, UpdateFilmRequest request);
    void deleteFilm(String slug);
    List<Film> getAllFilms(int page, int size);
    int getTotalFilms();
    List<Film> searchFilms(String query, int page, int size);
    int getTotalSearchResults(String query);
}