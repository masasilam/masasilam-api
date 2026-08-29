package com.masasilam.app.controller.monograph;

import com.masasilam.app.model.dto.monograph.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.monograph.MonographService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/monographs")
@RequiredArgsConstructor
public class MonographController {
    private final MonographService monographService;

    @GetMapping
    public ResponseEntity<DatatableResponse<MonographResponse>> getMonographs(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) int limit,
            @RequestParam(defaultValue = "editionDate") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sourceSlug,
            @RequestParam(required = false) Integer decade) {
        MonographSearchCriteria criteria = MonographSearchCriteria.builder()
                .searchQuery(q).sourceSlug(sourceSlug).decade(decade).status(null)
                .build();
        return ResponseEntity.ok(monographService.getMonographs(page, limit, sortField, sortOrder, criteria));
    }

    @GetMapping("/manage")
    public ResponseEntity<DatatableResponse<MonographResponse>> getMonographsForManagement(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(defaultValue = "editionDate") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        MonographSearchCriteria criteria = MonographSearchCriteria.builder()
                .searchQuery(q).status(status != null ? status : "all")
                .build();
        return ResponseEntity.ok(monographService.getMonographs(page, limit, sortField, sortOrder, criteria));
    }

    @GetMapping("/latest")
    public ResponseEntity<DataResponse<List<MonographResponse>>> getLatest(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(monographService.getLatestMonographs(limit));
    }

    @GetMapping("/scaffold")
    public ResponseEntity<DataResponse<MonographScaffoldResponse>> getScaffold(
            @RequestParam Long sourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(monographService.getScaffold(sourceId, from, to));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<MonographDetailResponse>> getBySlug(@PathVariable String slug, HttpServletRequest request) {
        monographService.trackView(slug, request);
        return ResponseEntity.ok(monographService.getMonographDetailBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<DataResponse<MonographDetailResponse>> create(@Valid @RequestBody CreateMonographRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monographService.createMonograph(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<MonographDetailResponse>> update(@PathVariable Long id, @Valid @RequestBody UpdateMonographRequest request) {
        return ResponseEntity.ok(monographService.updateMonograph(id, request));
    }

    @PutMapping("/{id}/structure")
    public ResponseEntity<DataResponse<MonographDetailResponse>> saveStructure(@PathVariable Long id, @Valid @RequestBody MonographStructureRequest request) {
        return ResponseEntity.ok(monographService.saveStructure(id, request));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<DataResponse<MonographValidationResponse>> validate(@PathVariable Long id) {
        return ResponseEntity.ok(monographService.validateMonograph(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DataResponse<MonographDetailResponse>> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateMonographStatusRequest request) {
        return ResponseEntity.ok(monographService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(monographService.deleteMonograph(id));
    }

    @GetMapping("/{id}/adjacent")
    public ResponseEntity<DataResponse<MonographAdjacentResponse>> getAdjacentMonographs(@PathVariable Long id) {
        return ResponseEntity.ok(monographService.getAdjacentMonographs(id));
    }
}