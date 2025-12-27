package com.naskah.demo.service;

import com.naskah.demo.model.entity.Genre;

import java.util.List;

public interface GenreService {
    List<Genre> getAllGenresWithBooks();
}
