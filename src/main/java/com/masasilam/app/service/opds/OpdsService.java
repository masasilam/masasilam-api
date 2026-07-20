package com.masasilam.app.service.opds;

import com.masasilam.app.model.dto.opds.OpdsFeed;

public interface OpdsService {
    OpdsFeed getRootCatalog();
    OpdsFeed getNewBooks(int page, int limit);
    OpdsFeed searchBooks(String query, int page, int limit);
    OpdsFeed getBooksByGenre(String genreSlug, int page, int limit);
    OpdsFeed getAllGenres();
}