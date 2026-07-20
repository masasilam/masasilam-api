package com.naskah.app.service.social;

import com.naskah.app.model.dto.request.social.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;
import java.util.List;

public interface ReadingListService {
    DataResponse<ReadingListResponse> createList(CreateReadingListRequest request);
    DataResponse<ReadingListResponse> updateList(Long listId, UpdateReadingListRequest request);
    DataResponse<Void> deleteList(Long listId);
    DataResponse<ReadingListResponse> getListDetail(Long listId);
    DataResponse<ReadingListResponse> getListBySlug(Long userId, String slug);
    DatatableResponse<ReadingListResponse> getMyLists(int page, int limit);
    DatatableResponse<ReadingListResponse> getUserLists(Long userId, int page, int limit);
    DatatableResponse<ReadingListResponse> getPublicLists(String search, String tag, int page, int limit);
    DataResponse<ReadingListItemResponse> addItem(Long listId, AddToReadingListRequest request);
    DataResponse<Void> removeItem(Long listId, String entityType, Long entityId);
    DataResponse<Void> reorderItems(Long listId, List<Long> orderedItemIds);
    DatatableResponse<ReadingListItemResponse> getItems(Long listId, int page, int limit);
    DataResponse<Void> likeList(Long listId);
    DataResponse<Void> unlikeList(Long listId);
    DataResponse<Void> followList(Long listId);
    DataResponse<Void> unfollowList(Long listId);
    DataResponse<ReadingListResponse> forkList(Long listId);
    DataResponse<List<ReadingListSummaryResponse>> getListsContainingEntity(String entityType, Long entityId);

    DataResponse<ReadingListResponse> getListBySlugOnly(String slug);
}