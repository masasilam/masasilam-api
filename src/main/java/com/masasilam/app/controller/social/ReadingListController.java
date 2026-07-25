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

    @PostMapping
    public ResponseEntity<DataResponse<ReadingListResponse>> createList(@Valid @RequestBody CreateReadingListRequest request) {
        DataResponse<ReadingListResponse> response = listService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/public")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getPublicLists(@RequestParam(required = false) String search,
                                                                                 @RequestParam(required = false) String tag,
                                                                                 @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                 @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getPublicLists(search, tag, page, limit));
    }

    @GetMapping("/me")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getMyLists(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                             @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getMyLists(page, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DatatableResponse<ReadingListResponse>> getUserLists(@PathVariable Long userId,
                                                                               @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                               @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getUserLists(userId, page, limit));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListBySlugOnly(@PathVariable String slug) {
        return ResponseEntity.ok(listService.getListBySlugOnly(slug));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListDetail(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.getListDetail(listId));
    }

    @GetMapping("/user/{userId}/slug/{slug}")
    public ResponseEntity<DataResponse<ReadingListResponse>> getListBySlug(@PathVariable Long userId,
                                                                           @PathVariable String slug) {
        return ResponseEntity.ok(listService.getListBySlug(userId, slug));
    }

    @PutMapping("/{listId}")
    public ResponseEntity<DataResponse<ReadingListResponse>> updateList(@PathVariable Long listId,
                                                                        @Valid @RequestBody UpdateReadingListRequest request) {
        return ResponseEntity.ok(listService.updateList(listId, request));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<DataResponse<Void>> deleteList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.deleteList(listId));
    }

    @GetMapping("/{listId}/items")
    public ResponseEntity<DatatableResponse<ReadingListItemResponse>> getItems(@PathVariable Long listId,
                                                                               @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                               @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(listService.getItems(listId, page, limit));
    }

    @PostMapping("/{listId}/items")
    public ResponseEntity<DataResponse<ReadingListItemResponse>> addItem(@PathVariable Long listId,
                                                                         @Valid @RequestBody AddToReadingListRequest request) {
        DataResponse<ReadingListItemResponse> response = listService.addItem(listId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{listId}/items")
    public ResponseEntity<DataResponse<Void>> removeItem(@PathVariable Long listId,
                                                         @RequestParam String entityType,
                                                         @RequestParam Long entityId) {
        return ResponseEntity.ok(listService.removeItem(listId, entityType, entityId));
    }

    @PutMapping("/{listId}/items/reorder")
    public ResponseEntity<DataResponse<Void>> reorderItems(@PathVariable Long listId,
                                                           @RequestBody List<Long> orderedItemIds) {
        return ResponseEntity.ok(listService.reorderItems(listId, orderedItemIds));
    }

    @PostMapping("/{listId}/like")
    public ResponseEntity<DataResponse<Void>> likeList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.likeList(listId));
    }

    @DeleteMapping("/{listId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.unlikeList(listId));
    }

    @PostMapping("/{listId}/follow")
    public ResponseEntity<DataResponse<Void>> followList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.followList(listId));
    }

    @DeleteMapping("/{listId}/follow")
    public ResponseEntity<DataResponse<Void>> unfollowList(@PathVariable Long listId) {
        return ResponseEntity.ok(listService.unfollowList(listId));
    }

    @PostMapping("/{listId}/fork")
    public ResponseEntity<DataResponse<ReadingListResponse>> forkList(@PathVariable Long listId) {
        DataResponse<ReadingListResponse> response = listService.forkList(listId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/containing")
    public ResponseEntity<DataResponse<List<ReadingListSummaryResponse>>> getListsContainingEntity(@RequestParam String entityType,
                                                                                                   @RequestParam Long entityId) {
        return ResponseEntity.ok(listService.getListsContainingEntity(entityType, entityId));
    }
}