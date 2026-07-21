package com.masasilam.app.mapper.author;

import com.masasilam.app.model.dto.response.AuthorResponse;
import com.masasilam.app.model.entity.Author;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthorMapper {
    Author findAuthorByName(@Param("name") String name);
    void updateAuthor(Author author);
    void insertAuthor(Author author);
    List<AuthorResponse> findAuthorsByBookId(@Param("bookId") Long bookId);
    List<Author> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit, @Param("search") String search, @Param("sortColumn") String sortColumn);
    int countAll(@Param("search") String search);
    Author findAuthorBySlug(@Param("slug") String slug);
    List<Author> findAllAuthors();
}