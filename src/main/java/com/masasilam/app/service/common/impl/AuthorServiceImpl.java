package com.masasilam.app.service.common.impl;

import com.masasilam.app.mapper.AuthorMapper;
import com.masasilam.app.model.entity.Author;
import com.masasilam.app.service.common.AuthorService;
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
