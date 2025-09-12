package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.AuthorResponse;
import com.naskah.demo.model.entity.Author;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthorMapper {
    Author findAuthorByName(@Param("name") String name);
    void updateAuthor(Author author);
    void insertAuthor(Author author);
    List<AuthorResponse> findAuthorsByBookId(@Param("bookId") Long bookId);
}
