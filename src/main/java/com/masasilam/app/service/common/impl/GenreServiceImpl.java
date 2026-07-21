package com.masasilam.app.service.common.impl;

import com.masasilam.app.mapper.book.GenreMapper;
import com.masasilam.app.model.entity.Genre;
import com.masasilam.app.service.common.GenreService;
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
