package com.masasilam.app.service.common;

import com.masasilam.app.model.entity.Genre;

import java.util.List;

public interface GenreService {
    List<Genre> getAllGenresWithBooks();
}
