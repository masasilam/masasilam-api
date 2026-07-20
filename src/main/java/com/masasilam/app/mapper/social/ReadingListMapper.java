package com.masasilam.app.mapper.social;

import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReadingListMapper {
    void insertList(ReadingList list);
    void updateList(ReadingList list);
    void softDeleteList(Long id);
    ReadingList findById(Long id);
    ReadingList findBySlug(@Param("userId") Long userId, @Param("slug") String slug);
    ReadingListResponse getListDetail(@Param("listId") Long listId, @Param("currentUserId") Long currentUserId);
    List<ReadingListResponse> findByUser(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId, @Param("offset") int offset, @Param("limit") int limit);
    List<ReadingListResponse> findPublicLists(@Param("search") String search, @Param("tag") String tag, @Param("offset") int offset, @Param("limit") int limit);
    int countByUser(Long userId);
    int countPublicLists(@Param("search") String search, @Param("tag") String tag);
    void insertItem(ReadingListItem item);
    void deleteItem(@Param("listId") Long listId, @Param("entityType") String entityType, @Param("entityId") Long entityId);
    void deleteItemById(Long id);
    ReadingListItem findItem(@Param("listId") Long listId, @Param("entityType") String entityType, @Param("entityId") Long entityId);
    List<ReadingListItemResponse> findItemsByList(@Param("listId") Long listId, @Param("offset") int offset, @Param("limit") int limit);
    void updateItemOrder(@Param("itemId") Long itemId, @Param("sortOrder") int sortOrder);
    int countItemsByList(Long listId);
    void insertLike(@Param("listId") Long listId, @Param("userId") Long userId);
    void deleteLike(@Param("listId") Long listId, @Param("userId") Long userId);
    boolean isLiked(@Param("listId") Long listId, @Param("userId") Long userId);
    void incrementLikeCount(Long listId);
    void decrementLikeCount(Long listId);
    void insertFollow(@Param("listId") Long listId, @Param("userId") Long userId);
    void deleteFollow(@Param("listId") Long listId, @Param("userId") Long userId);
    boolean isFollowed(@Param("listId") Long listId, @Param("userId") Long userId);
    void incrementForkCount(Long listId);
    void incrementViewCount(Long listId);
    List<ReadingListSummaryResponse> findListsContainingEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId, @Param("currentUserId") Long currentUserId);
    ReadingList findBySlugGlobal(@Param("slug") String slug);
    Integer getMaxSortOrder(@Param("listId") Long listId);
}