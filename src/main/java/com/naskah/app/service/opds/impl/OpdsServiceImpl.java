package com.naskah.app.service.opds.impl;

import com.naskah.app.mapper.BookMapper;
import com.naskah.app.mapper.GenreMapper;
import com.naskah.app.model.dto.BookSearchCriteria;
import com.naskah.app.model.dto.opds.*;
import com.naskah.app.model.dto.response.BookResponse;
import com.naskah.app.model.entity.Genre;
import com.naskah.app.service.opds.OpdsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpdsServiceImpl implements OpdsService {

    private final BookMapper bookMapper;
    private final GenreMapper genreMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.api-url}")
    private String apiUrl;

    private static final String OPDS_CATALOG_MIME = "application/atom+xml;profile=opds-catalog;kind=navigation";
    private static final String OPDS_ACQUISITION_MIME = "application/atom+xml;profile=opds-catalog;kind=acquisition";

    @Override
    public OpdsFeed getRootCatalog() {
        OpdsFeed feed = new OpdsFeed();
        feed.setId(apiUrl + "/opds");
        feed.setTitle("Masasilam – Perpustakaan Digital");
        feed.setUpdated(Instant.now().toString());

        feed.setLinks(List.of(
                new OpdsLink("self", apiUrl + "/opds", OPDS_CATALOG_MIME),
                new OpdsLink("start", apiUrl + "/opds", OPDS_CATALOG_MIME),
                new OpdsLink("search",
                        apiUrl + "/opds/search?q={searchTerms}&page={startPage?}&limit={count?}",
                        "application/atom+xml")
        ));

        List<OpdsEntry> entries = new ArrayList<>();
        entries.add(buildNavEntry(
                "buku-terbaru",
                "Buku Terbaru",
                "Koleksi buku terbaru yang baru saja ditambahkan",
                apiUrl + "/opds/new",
                OPDS_ACQUISITION_MIME
        ));
        entries.add(buildNavEntry(
                "semua-genre",
                "Telusuri per Genre",
                "Jelajahi buku berdasarkan genre dan kategori",
                apiUrl + "/opds/genres",
                OPDS_CATALOG_MIME
        ));

        feed.setEntries(entries);
        return feed;
    }

    @Override
    public OpdsFeed getNewBooks(int page, int limit) {
        BookSearchCriteria criteria = BookSearchCriteria.builder().build();
        int offset = (page - 1) * limit;

        List<BookResponse> books = bookMapper.getBookListWithAdvancedFilters(
                criteria, offset, limit, "b.updated_at", "DESC"
        );
        int totalCount = bookMapper.countBooksWithAdvancedFilters(criteria);

        OpdsFeed feed = new OpdsFeed();
        feed.setId(apiUrl + "/opds/new");
        feed.setTitle("Buku Terbaru – Masasilam");
        feed.setUpdated(Instant.now().toString());
        feed.setTotalResults(totalCount);
        feed.setItemsPerPage(limit);
        feed.setStartIndex((page - 1) * limit + 1);

        List<OpdsLink> links = new ArrayList<>();
        links.add(new OpdsLink("self", apiUrl + "/opds/new?page=" + page + "&limit=" + limit, OPDS_ACQUISITION_MIME));
        links.add(new OpdsLink("start", apiUrl + "/opds", OPDS_CATALOG_MIME));
        links.add(new OpdsLink("up", apiUrl + "/opds", OPDS_CATALOG_MIME));

        // Pagination links
        if (page > 1) {
            links.add(new OpdsLink("previous",
                    apiUrl + "/opds/new?page=" + (page - 1) + "&limit=" + limit,
                    OPDS_ACQUISITION_MIME));
        }
        if ((long) page * limit < totalCount) {
            links.add(new OpdsLink("next",
                    apiUrl + "/opds/new?page=" + (page + 1) + "&limit=" + limit,
                    OPDS_ACQUISITION_MIME));
        }

        feed.setLinks(links);
        feed.setEntries(books.stream().map(this::toOpdsEntry).toList());
        return feed;
    }

    @Override
    public OpdsFeed searchBooks(String query, int page, int limit) {
        BookSearchCriteria criteria = BookSearchCriteria.builder()
                .searchTitle(query)
                .build();
        int offset = (page - 1) * limit;

        List<BookResponse> books = bookMapper.getBookListWithAdvancedFilters(
                criteria, offset, limit, "b.updated_at", "DESC"
        );
        int totalCount = bookMapper.countBooksWithAdvancedFilters(criteria);

        OpdsFeed feed = new OpdsFeed();
        feed.setId(apiUrl + "/opds/search?q=" + query);
        feed.setTitle("Hasil pencarian: " + query);
        feed.setUpdated(Instant.now().toString());
        feed.setTotalResults(totalCount);
        feed.setItemsPerPage(limit);
        feed.setStartIndex(offset + 1);

        feed.setLinks(List.of(
                new OpdsLink("self",
                        apiUrl + "/opds/search?q=" + query + "&page=" + page + "&limit=" + limit,
                        OPDS_ACQUISITION_MIME),
                new OpdsLink("up", apiUrl + "/opds", OPDS_CATALOG_MIME)
        ));

        feed.setEntries(books.stream().map(this::toOpdsEntry).toList());
        return feed;
    }

    @Override
    public OpdsFeed getBooksByGenre(String genreSlug, int page, int limit) {
        BookSearchCriteria criteria = BookSearchCriteria.builder()
                .genre(genreSlug)
                .build();
        int offset = (page - 1) * limit;

        List<BookResponse> books = bookMapper.getBookListWithAdvancedFilters(
                criteria, offset, limit, "b.updated_at", "DESC"
        );
        int totalCount = bookMapper.countBooksWithAdvancedFilters(criteria);

        OpdsFeed feed = new OpdsFeed();
        feed.setId(apiUrl + "/opds/genre/" + genreSlug);
        feed.setTitle("Genre: " + genreSlug);
        feed.setUpdated(Instant.now().toString());
        feed.setTotalResults(totalCount);
        feed.setItemsPerPage(limit);
        feed.setStartIndex(offset + 1);

        List<OpdsLink> links = new ArrayList<>();
        links.add(new OpdsLink("self",
                apiUrl + "/opds/genre/" + genreSlug + "?page=" + page,
                OPDS_ACQUISITION_MIME));
        links.add(new OpdsLink("up", apiUrl + "/opds/genres", OPDS_CATALOG_MIME));

        if (page > 1) {
            links.add(new OpdsLink("previous",
                    apiUrl + "/opds/genre/" + genreSlug + "?page=" + (page - 1),
                    OPDS_ACQUISITION_MIME));
        }
        if ((long) page * limit < totalCount) {
            links.add(new OpdsLink("next",
                    apiUrl + "/opds/genre/" + genreSlug + "?page=" + (page + 1),
                    OPDS_ACQUISITION_MIME));
        }

        feed.setLinks(links);
        feed.setEntries(books.stream().map(this::toOpdsEntry).toList());
        return feed;
    }

    @Override
    public OpdsFeed getAllGenres() {
        List<Genre> genres = genreMapper.findAllWithBookCount();

        OpdsFeed feed = new OpdsFeed();
        feed.setId(apiUrl + "/opds/genres");
        feed.setTitle("Genre – Masasilam");
        feed.setUpdated(Instant.now().toString());
        feed.setLinks(List.of(
                new OpdsLink("self", apiUrl + "/opds/genres", OPDS_CATALOG_MIME),
                new OpdsLink("up", apiUrl + "/opds", OPDS_CATALOG_MIME)
        ));

        List<OpdsEntry> entries = genres.stream()
                .map(g -> buildNavEntry(
                        "genre-" + g.getSlug(),
                        g.getName(),
                        g.getBookCount() != null ? g.getBookCount() + " buku" : null,
                        apiUrl + "/opds/genre/" + g.getSlug(),
                        OPDS_ACQUISITION_MIME
                ))
                .toList();

        feed.setEntries(entries);
        return feed;
    }

    // ============ PRIVATE HELPERS ============

    private OpdsEntry toOpdsEntry(BookResponse book) {
        OpdsEntry entry = new OpdsEntry();
        entry.setId(apiUrl + "/opds/book/" + book.getSlug());
        entry.setTitle(book.getTitle());

        String updated = book.getUpdatedAt() != null
                ? book.getUpdatedAt().toString()
                : Instant.now().toString();
        entry.setUpdated(updated);
        entry.setSummary(book.getDescription());

        // Parse author names and slugs from delimited strings
        if (book.getAuthorNames() != null && !book.getAuthorNames().isBlank()) {
            String[] names = book.getAuthorNames().split(",");
            String[] slugs = book.getAuthorSlugs() != null
                    ? book.getAuthorSlugs().split(",")
                    : new String[0];

            List<OpdsAuthor> authors = new ArrayList<>();
            for (int i = 0; i < names.length; i++) {
                String name = names[i].trim();
                String slug = i < slugs.length ? slugs[i].trim() : "";
                authors.add(new OpdsAuthor(
                        name,
                        slug.isBlank() ? null : baseUrl + "/authors/" + slug
                ));
            }
            entry.setAuthors(authors);
        }

        List<OpdsLink> links = new ArrayList<>();

        if (book.getCoverImageUrl() != null) {
            links.add(new OpdsLink(
                    "http://opds-spec.org/image",
                    book.getCoverImageUrl(),
                    "image/jpeg"
            ));
            links.add(new OpdsLink(
                    "http://opds-spec.org/image/thumbnail",
                    book.getCoverImageUrl(),
                    "image/jpeg"
            ));
        }

        links.add(new OpdsLink(
                "alternate",
                baseUrl + "/books/" + book.getSlug(),
                "text/html",
                book.getTitle()
        ));

        links.add(new OpdsLink(
                "http://opds-spec.org/acquisition",
                apiUrl + "/api/books/" + book.getSlug() + "/download",
                "application/epub+zip"
        ));

        entry.setLinks(links);
        return entry;
    }

    private OpdsEntry buildNavEntry(String id, String title, String summary, String href, String mimeType) {
        OpdsEntry entry = new OpdsEntry();
        entry.setId(apiUrl + "/opds/" + id);
        entry.setTitle(title);
        entry.setSummary(summary);
        entry.setUpdated(Instant.now().toString());
        entry.setNavigation(true);
        entry.setLinks(List.of(new OpdsLink("subsection", href, mimeType)));
        return entry;
    }
}