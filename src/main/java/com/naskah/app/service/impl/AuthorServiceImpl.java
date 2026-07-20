package com.naskah.app.service.impl;

import com.naskah.app.mapper.AuthorMapper;
import com.naskah.app.model.entity.Author;
import com.naskah.app.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {
    private final AuthorMapper authorMapper;

    @Override
    public List<Author> getAllAuthors() {
        return authorMapper.findAllAuthors();
    }
}
