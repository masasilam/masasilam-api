package com.naskah.demo.service.opds;

import com.naskah.demo.model.dto.opds.OpdsFeed;

public interface OpdsService {
    OpdsFeed getRootCatalog();
    OpdsFeed getNewBooks(int page, int limit);
    OpdsFeed searchBooks(String query, int page, int limit);
    OpdsFeed getBooksByGenre(String genreSlug, int page, int limit);
    OpdsFeed getAllGenres();
}