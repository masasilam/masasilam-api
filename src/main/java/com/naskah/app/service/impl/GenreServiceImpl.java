package com.naskah.app.service.impl;

import com.naskah.app.mapper.GenreMapper;
import com.naskah.app.model.entity.Genre;
import com.naskah.app.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {
    private final GenreMapper genreMapper;

    @Override
    public List<Genre> getAllGenresWithBooks() {
        return genreMapper.findAllWithBookCount();
    }
}
