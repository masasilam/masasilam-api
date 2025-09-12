package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.BookRequest;
import com.naskah.demo.model.dto.response.BookResponse;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.DatatableResponse;
import com.naskah.demo.model.dto.response.ReadingResponse;
import com.naskah.demo.service.BookService;
import com.naskah.demo.util.FileTypeUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BookResponse>> createBook(@Valid @ModelAttribute BookRequest request) {

        DataResponse<BookResponse> response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<BookResponse>> getBookDetail(@PathVariable String slug) {
        DataResponse<BookResponse> response = bookService.getBookDetailBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/read")
    public ResponseEntity<DataResponse<ReadingResponse>> startReading(@PathVariable String slug) {
        DataResponse<ReadingResponse> response = bookService.startReading(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<BookResponse>> getBooksPaginated(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) int limit, @RequestParam(defaultValue = "updateAt", required = false) String sortField, @RequestParam(defaultValue = "DESC", required = false) String sortOrder, @RequestParam(required = false) String searchTitle, @RequestParam(required = false) Long seriesId, @RequestParam(required = false) Long genreId, @RequestParam(required = false) Long subGenreId) {

        DatatableResponse<BookResponse> response = bookService.getPaginatedBooks(page, limit, sortField, sortOrder, searchTitle, seriesId, genreId, subGenreId);
        return ResponseEntity.ok(response);
    }

//
//    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<DataResponse<Book>> update(
//            @RequestParam String id,
//            @RequestPart("ebook") @Valid Book book,
//            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
//        DataResponse<Book> response = ebookService.update(id, book, file);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping
//    public ResponseEntity<DefaultResponse> delete(@RequestParam String id) throws IOException {
//        DefaultResponse response = ebookService.delete(id);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/download")
//    public ResponseEntity<Resource> download(@RequestParam String id) throws MalformedURLException {
//        Resource resource = ebookService.downloadEbook(id);
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
//                .body(resource);
//    }
//
//    @GetMapping("/read")
//    public ResponseEntity<Resource> readEbook(@RequestParam String id) throws IOException {
//        Resource resource = ebookService.readEbook(id);
//        String contentType = fileTypeUtil.contentType(resource.getFilename());
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(contentType))
//                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
//                .body(resource);
//    }
}
