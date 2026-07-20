package com.naskah.app.service;

import com.naskah.app.model.entity.Genre;

import java.util.List;

public interface GenreService {
    List<Genre> getAllGenresWithBooks();
}
