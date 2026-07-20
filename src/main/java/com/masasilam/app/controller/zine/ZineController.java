package com.masasilam.app.controller.zine;

import com.masasilam.app.model.dto.ZineSearchCriteria;
import com.masasilam.app.model.dto.request.ZineRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.Zine;
import com.masasilam.app.service.zine.ZineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/zines")
@RequiredArgsConstructor
public class ZineController {
    private final ZineService zineService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<ZineResponse>> createZine(@Valid @ModelAttribute ZineRequest request) {
        DataResponse<ZineResponse> response = zineService.createZine(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<ZineResponse>> getZineDetail(@PathVariable String slug, HttpServletRequest request) {
        DataResponse<ZineResponse> response = zineService.getZineDetailBySlug(slug, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<ZineResponse>> getZinesPaginated(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                             @RequestParam(defaultValue = "12") @Min(1) int limit,
                                                                             @RequestParam(defaultValue = "updateAt") String sortField,
                                                                             @RequestParam(defaultValue = "DESC") String sortOrder,
                                                                             @RequestParam(required = false) String searchTitle,
                                                                             @RequestParam(required = false) String searchInZine,
                                                                             @RequestParam(required = false) String authorName,
                                                                             @RequestParam(required = false) String contributor,
                                                                             @RequestParam(required = false) String genre,
                                                                             @RequestParam(required = false) Integer volume,
                                                                             @RequestParam(required = false) String issueNumber,
                                                                             @RequestParam(required = false) Integer minChapters,
                                                                             @RequestParam(required = false) Integer maxChapters,
                                                                             @RequestParam(required = false) Long minFileSize,
                                                                             @RequestParam(required = false) Long maxFileSize,
                                                                             @RequestParam(required = false) Integer publicationYearFrom,
                                                                             @RequestParam(required = false) Integer publicationYearTo,
                                                                             @RequestParam(required = false) String difficultyLevel,
                                                                             @RequestParam(required = false) String fileFormat,
                                                                             @RequestParam(required = false) Boolean isFeatured,
                                                                             @RequestParam(required = false) Integer languageId,
                                                                             @RequestParam(required = false) Double minRating,
                                                                             @RequestParam(required = false) Integer minViewCount,
                                                                             @RequestParam(required = false) Integer minReadCount) {
        ZineSearchCriteria criteria = ZineSearchCriteria.builder()
                .searchTitle(searchTitle)
                .searchInZine(searchInZine)
                .authorName(authorName)
                .contributor(contributor)
                .genre(genre)
                .volume(volume)
                .issueNumber(issueNumber)
                .minPages(minChapters)
                .maxPages(maxChapters)
                .minFileSize(minFileSize)
                .maxFileSize(maxFileSize)
                .publicationYearFrom(publicationYearFrom)
                .publicationYearTo(publicationYearTo)
                .difficultyLevel(difficultyLevel)
                .fileFormat(fileFormat)
                .isFeatured(isFeatured)
                .languageId(languageId)
                .minRating(minRating)
                .minViewCount(minViewCount)
                .minReadCount(minReadCount)
                .build();
        DatatableResponse<ZineResponse> response = zineService.getPaginatedZines(page, limit, sortField, sortOrder, criteria);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Zine>> updateZine(@PathVariable Long id,
                                                         @RequestPart("zine") @Valid Zine zine,
                                                         @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        DataResponse<Zine> response = zineService.update(id, zine, file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DefaultResponse> deleteZine(@PathVariable Long id) throws IOException {
        DefaultResponse response = zineService.delete(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/download")
    public ResponseEntity<?> downloadZine(@PathVariable String slug, HttpServletRequest request) {
        return zineService.getDownloadUrl(slug, request);
    }

    @GetMapping("/genres")
    public ResponseEntity<DataResponse<List<GenreResponse>>> getAllGenres(@RequestParam(defaultValue = "false") boolean includeZineCount) {
        DataResponse<List<GenreResponse>> response = zineService.getAllGenres(includeZineCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/authors")
    public ResponseEntity<DatatableResponse<AuthorResponse>> getAllAuthors(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                           @RequestParam(defaultValue = "20") @Min(1) int limit,
                                                                           @RequestParam(required = false) String search,
                                                                           @RequestParam(defaultValue = "name") String sortBy) {
        DatatableResponse<AuthorResponse> response = zineService.getAllAuthors(page, limit, search, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contributors")
    public ResponseEntity<DatatableResponse<ContributorResponse>> getAllContributors(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                     @RequestParam(defaultValue = "20") @Min(1) int limit,
                                                                                     @RequestParam(required = false) String role,
                                                                                     @RequestParam(required = false) String search) {
        DatatableResponse<ContributorResponse> response = zineService.getAllContributors(page, limit, role, search);
        return ResponseEntity.ok(response);
    }
}