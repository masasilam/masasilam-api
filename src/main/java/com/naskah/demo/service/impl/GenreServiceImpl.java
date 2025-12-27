package com.naskah.demo.service.impl;

import com.naskah.demo.mapper.GenreMapper;
import com.naskah.demo.model.entity.Genre;
import com.naskah.demo.service.GenreService;
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
