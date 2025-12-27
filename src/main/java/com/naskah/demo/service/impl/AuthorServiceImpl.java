package com.naskah.demo.service.impl;

import com.naskah.demo.mapper.AuthorMapper;
import com.naskah.demo.model.entity.Author;
import com.naskah.demo.service.AuthorService;
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
