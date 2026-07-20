package com.naskah.app.controller.film;

import com.naskah.app.model.dto.request.AddFilmRequest;
import com.naskah.app.model.dto.request.UpdateFilmRequest;
import com.naskah.app.model.dto.response.DatatableResponse;
import com.naskah.app.model.dto.response.FilmWatchlistResponse;
import com.naskah.app.model.film.Film;
import com.naskah.app.model.film.FilmDetail;
import com.naskah.app.service.film.FilmReactionService;
import com.naskah.app.service.film.FilmService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/films")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FilmController {
    private final FilmService filmService;
    private final FilmReactionService filmReactionService;
    private static final String MESSAGE = "message";
    private static final String ERROR = "error";

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllFilms(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        List<Film> films = filmService.getAllFilms(page, size);
        int total = filmService.getTotalFilms();

        Map<String, Object> res = new HashMap<>();
        res.put("films", films);
        res.put("currentPage", page);
        res.put("totalItems", total);
        res.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchFilms(@RequestParam String q,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        List<Film> films = filmService.searchFilms(q, page, size);
        int total = filmService.getTotalSearchResults(q);

        Map<String, Object> res = new HashMap<>();
        res.put("films", films);
        res.put("query", q);
        res.put("currentPage", page);
        res.put("totalItems", total);
        res.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/watchlist/me")
    public ResponseEntity<DatatableResponse<FilmWatchlistResponse>> getMyWatchlist(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                   @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(filmReactionService.getMyWatchlist(page, limit));
    }

    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmBySlug(@PathVariable String slug) {
        FilmDetail film = filmService.getFilmDetailBySlug(slug);
        return film == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(film);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addFilm(@Valid @RequestBody AddFilmRequest request) {
        try {
            Film saved = filmService.addFilm(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "slug", saved.getSlug(),
                    MESSAGE, "Film berhasil ditambahkan"
            ));
        } catch (Exception e) {
            log.error("Gagal tambah film: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR, e.getMessage()));
        }
    }

    @PutMapping(value = "/{slug}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateFilm(@PathVariable String slug, @RequestBody UpdateFilmRequest request) {
        try {
            Film updated = filmService.updateFilm(slug, request);
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "slug", updated.getSlug(),
                    MESSAGE, "Film berhasil diperbarui"
            ));
        } catch (Exception e) {
            log.error("Gagal update film {}: {}", slug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR, e.getMessage()));
        }
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> deleteFilm(@PathVariable String slug) {
        try {
            filmService.deleteFilm(slug);
            return ResponseEntity.ok(Map.of(MESSAGE, "Film berhasil dihapus"));
        } catch (Exception e) {
            log.error("Gagal hapus film {}: {}", slug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR, e.getMessage()));
        }
    }
}