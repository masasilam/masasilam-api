package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.ReadingListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/lists")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService listService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/social/lists
     * Buat reading list baru
     */
    @PostMapping
    public ResponseEntity<DataResponse<ReadingListResponse>> createList(
            @Valid @RequestBody CreateReadingListRequest request) {
        DataResponse<ReadingListResponse> response = listService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/social/lists/public
     * Semua reading list publik (discovery)
     */
    @GetMapping("/public")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getPublicLists(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getPublicLists(search, tag, page, limit));
    }

    /**
     * GET /api/social/lists/me
     * Reading list milik saya
     */
    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getMyLists(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getMyLists(page, limit));
    }

    /**
     * GET /api/social/lists/user/{userId}
     * Reading list milik user tertentu
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getUserLists(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getUserLists(userId, page, limit));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/social/lists/slug/{slug} - HARUS DI ATAS /{listId}
     * Detail reading list by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListBySlugOnly(
            @PathVariable String slug) {
        return ResponseEntity.ok(listService.getListBySlugOnly(slug));
    }

    /**
     * GET /api/social/lists/{listId}
     * Detail reading list by ID
     */
    @GetMapping("/{listId}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListDetail(
            @PathVariable Long listId) {
        return ResponseEntity.ok(listService.getListDetail(listId));
    }

    /**
     * GET /api/social/lists/user/{userId}/slug/{slug}
     * Detail reading list by user + slug
     */
    @GetMapping("/user/{userId}/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListBySlug(
            @PathVariable Long userId,
            @PathVariable String slug) {
        return ResponseEntity.ok(listService.getListBySlug(userId, slug));
    }

    /**
     * PUT /api/social/lists/{listId}
     * Update reading list
     */
    @PutMapping("/{listId}")
    public ResponseEntity<DataResponse<ReadingListResponse>> updateList(
            @PathVariable Long listId,
            @Valid @RequestBody UpdateReadingListRequest request) {
        return ResponseEntity.ok(listService.updateList(listId, request));
    }

    /**
     * DELETE /api/social/lists/{listId}
     * Hapus reading list
     */
    @DeleteMapping("/{listId}")
    public ResponseEntity<DataResponse<Void>> deleteList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.deleteList(listId));
    }

    // ── ITEMS ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/social/lists/{listId}/items
     * Ambil item dalam list
     */
    @GetMapping("/{listId}/items")
    public ResponseEntity<DatatableResponse<ReadingListItemResponse>> getItems(
            @PathVariable Long listId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getItems(listId, page, limit));
    }

    /**
     * POST /api/social/lists/{listId}/items
     * Tambah item ke list
     */
    @PostMapping("/{listId}/items")
    public ResponseEntity<DataResponse<ReadingListItemResponse>> addItem(
            @PathVariable Long listId,
            @Valid @RequestBody AddToReadingListRequest request) {
        DataResponse<ReadingListItemResponse> response = listService.addItem(listId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/social/lists/{listId}/items
     * Hapus item dari list
     * Query params: entityType, entityId
     */
    @DeleteMapping("/{listId}/items")
    public ResponseEntity<DataResponse<Void>> removeItem(
            @PathVariable Long listId,
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(listService.removeItem(listId, entityType, entityId));
    }

    /**
     * PUT /api/social/lists/{listId}/items/reorder
     * Reorder item dalam list
     * Body: list of item IDs in new order
     */
    @PutMapping("/{listId}/items/reorder")
    public ResponseEntity<DataResponse<Void>> reorderItems(
            @PathVariable Long listId,
            @RequestBody List<Long> orderedItemIds) {
        return ResponseEntity.ok(listService.reorderItems(listId, orderedItemIds));
    }

    // ── LIKES & FOLLOWS ───────────────────────────────────────────────────────

    /**
     * POST /api/social/lists/{listId}/like
     */
    @PostMapping("/{listId}/like")
    public ResponseEntity<DataResponse<Void>> likeList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.likeList(listId));
    }

    /**
     * DELETE /api/social/lists/{listId}/like
     */
    @DeleteMapping("/{listId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.unlikeList(listId));
    }

    /**
     * POST /api/social/lists/{listId}/follow
     * Subscribe ke list (dapat notif saat diupdate)
     */
    @PostMapping("/{listId}/follow")
    public ResponseEntity<DataResponse<Void>> followList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.followList(listId));
    }

    /**
     * DELETE /api/social/lists/{listId}/follow
     */
    @DeleteMapping("/{listId}/follow")
    public ResponseEntity<DataResponse<Void>> unfollowList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.unfollowList(listId));
    }

    // ── FORK ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/social/lists/{listId}/fork
     * Fork list milik orang lain
     */
    @PostMapping("/{listId}/fork")
    public ResponseEntity<DataResponse<ReadingListResponse>> forkList(
            @PathVariable Long listId) {
        DataResponse<ReadingListResponse> response = listService.forkList(listId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── ENTITY DISCOVERY ──────────────────────────────────────────────────────

    /**
     * GET /api/social/lists/containing?entityType=BOOK&entityId=123
     * Temukan reading list yang mengandung konten tertentu
     */
    @GetMapping("/containing")
    public ResponseEntity<DataResponse<List<ReadingListSummaryResponse>>> getListsContainingEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(listService.getListsContainingEntity(entityType, entityId));
    }
}