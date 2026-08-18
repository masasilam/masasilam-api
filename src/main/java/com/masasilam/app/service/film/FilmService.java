package com.masasilam.app.service.film;

import com.masasilam.app.model.dto.request.AddFilmRequest;
import com.masasilam.app.model.dto.request.UpdateFilmRequest;
import com.masasilam.app.model.entity.film.Film;
import com.masasilam.app.model.entity.film.FilmDetail;
import jakarta.servlet.http.HttpServletRequest;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FilmService {
    FilmDetail getFilmDetailBySlug(String slug, HttpServletRequest request) throws NoSuchAlgorithmException;
    Film addFilm(AddFilmRequest request);
    Film updateFilm(String slug, UpdateFilmRequest request);
    void deleteFilm(String slug);
    List<Film> getAllFilms(int page, int size, String sortColumn, String sortType);
    int getTotalFilms();
    List<Film> searchFilms(String query, int page, int size);
    int getTotalSearchResults(String query);
}