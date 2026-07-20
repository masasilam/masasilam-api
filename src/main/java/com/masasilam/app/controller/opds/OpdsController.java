package com.masasilam.app.controller.opds;

import com.masasilam.app.model.dto.opds.OpdsFeed;
import com.masasilam.app.service.opds.OpdsService;
import com.masasilam.app.util.opds.OpdsXmlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/opds")
@RequiredArgsConstructor
public class OpdsController {
    private final OpdsService opdsService;
    private final OpdsXmlBuilder xmlBuilder;

    private static final String OPDS_CONTENT_TYPE = "application/atom+xml;profile=opds-catalog;charset=UTF-8";

    @GetMapping
    public ResponseEntity<String> rootCatalog() {
        OpdsFeed feed = opdsService.getRootCatalog();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(OPDS_CONTENT_TYPE)).body(xmlBuilder.buildFeed(feed));
    }

    @GetMapping("/new")
    public ResponseEntity<String> newBooks(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int limit) {
        OpdsFeed feed = opdsService.getNewBooks(page, limit);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(OPDS_CONTENT_TYPE)).body(xmlBuilder.buildFeed(feed));
    }

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String q,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int limit) {
        log.info("OPDS search: query='{}', page={}", q, page);
        OpdsFeed feed = opdsService.searchBooks(q, page, limit);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(OPDS_CONTENT_TYPE)).body(xmlBuilder.buildFeed(feed));
    }

    @GetMapping("/genres")
    public ResponseEntity<String> genres() {
        OpdsFeed feed = opdsService.getAllGenres();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(OPDS_CONTENT_TYPE)).body(xmlBuilder.buildFeed(feed));
    }

    @GetMapping("/genre/{slug}")
    public ResponseEntity<String> booksByGenre(@PathVariable String slug,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int limit) {
        OpdsFeed feed = opdsService.getBooksByGenre(slug, limit, page);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(OPDS_CONTENT_TYPE)).body(xmlBuilder.buildFeed(feed));
    }
}