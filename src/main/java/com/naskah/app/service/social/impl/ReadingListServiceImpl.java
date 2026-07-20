package com.naskah.app.service.social.impl;

import com.naskah.app.exception.custom.*;
import com.naskah.app.mapper.BookMapper;
import com.naskah.app.mapper.FilmMapper;
import com.naskah.app.mapper.NewspaperMapper;
import com.naskah.app.mapper.ZineMapper;
import com.naskah.app.mapper.UserMapper;
import com.naskah.app.mapper.social.*;
import com.naskah.app.model.dto.request.social.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;
import com.naskah.app.model.entity.User;
import com.naskah.app.model.entity.social.*;
import com.naskah.app.service.social.*;
import com.naskah.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingListServiceImpl implements ReadingListService {

    private final ReadingListMapper listMapper;
    private final UserMapper userMapper;
    private final ActivityFeedService feedService;
    private final NotificationService notificationService;
    private final HeaderHolder headerHolder;
    private final BookMapper bookMapper;
    private final FilmMapper filmMapper;
    private final ZineMapper zineMapper;
    private final NewspaperMapper newspaperMapper;

    private static final String SUCCESS = "Success";

    // ── Auth ─────────────────────────────────────────────────────────────────
    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long currentUserIdOrNull() {
        try {
            String u = headerHolder.getUsername();
            if (u == null || u.isBlank()) return null;
            User user = userMapper.findUserByUsername(u);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<ReadingListResponse> createList(CreateReadingListRequest request) {
        User me = requireAuth();

        String slug = generateSlug(request.getTitle());
        // ensure unique slug per user
        String finalSlug = slug;
        int counter = 2;
        while (listMapper.findBySlug(me.getId(), finalSlug) != null) {
            finalSlug = slug + "-" + counter++;
        }

        ReadingList list = new ReadingList();
        list.setUserId(me.getId());
        list.setTitle(request.getTitle());
        list.setSlug(finalSlug);
        list.setDescription(request.getDescription());
        list.setVisibility(request.getVisibility() != null ? request.getVisibility() : "public");

        // PERBAIKAN: Handle tags dengan benar untuk PostgreSQL array
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            // PostgreSQL array format: {"tag1","tag2","tag3"}
            String tagsArray = request.getTags().stream()
                    .map(tag -> "\"" + tag.replace("\"", "\\\"") + "\"") // Escape quotes
                    .collect(Collectors.joining(",", "{", "}"));
            list.setTags(tagsArray);
        } else {
            list.setTags(null); // Jangan set string kosong, gunakan null
        }

        listMapper.insertList(list);

        // Publish activity
        feedService.publishActivity(me.getId(), "created_reading_list", "READING_LIST",
                list.getId(), finalSlug, request.getTitle(), null, "{}", list.getVisibility());

        ReadingListResponse response = listMapper.getListDetail(list.getId(), me.getId());
        return new DataResponse<>(SUCCESS, "Reading list created",
                HttpStatus.CREATED.value(), response);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<ReadingListResponse> updateList(Long listId, UpdateReadingListRequest request) {
        User me = requireAuth();
        ReadingList list = listMapper.findById(listId);
        if (list == null) throw new DataNotFoundException();
        if (!list.getUserId().equals(me.getId())) throw new ForbiddenException();

        if (request.getTitle() != null) list.setTitle(request.getTitle());
        if (request.getDescription() != null) list.setDescription(request.getDescription());
        if (request.getVisibility() != null) list.setVisibility(request.getVisibility());
        if (request.getTags() != null) list.setTags(String.join(",", request.getTags()));

        listMapper.updateList(list);
        ReadingListResponse response = listMapper.getListDetail(listId, me.getId());
        return new DataResponse<>(SUCCESS, "Reading list updated", HttpStatus.OK.value(), response);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<Void> deleteList(Long listId) {
        User me = requireAuth();
        ReadingList list = listMapper.findById(listId);
        if (list == null) throw new DataNotFoundException();
        if (!list.getUserId().equals(me.getId())) throw new ForbiddenException();
        listMapper.softDeleteList(listId);
        return new DataResponse<>(SUCCESS, "Reading list deleted", HttpStatus.OK.value(), null);
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    @Override
    public DataResponse<ReadingListResponse> getListDetail(Long listId) {
        Long currentUserId = currentUserIdOrNull();
        ReadingListResponse response = listMapper.getListDetail(listId, currentUserId);
        if (response == null) throw new DataNotFoundException();

        // attach items preview (first 10)
        List<ReadingListItemResponse> items = listMapper.findItemsByList(listId, 0, 10);
        // Enrich items dengan entity details jika diperlukan
        enrichItemsWithEntityDetails(items);
        response.setItems(items);

        listMapper.incrementViewCount(listId);
        return new DataResponse<>(SUCCESS, "List retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<ReadingListResponse> getListBySlug(Long userId, String slug) {
        ReadingList entity = listMapper.findBySlug(userId, slug);
        if (entity == null) throw new DataNotFoundException();
        return getListDetail(entity.getId());
    }

    @Override
    public DatatableResponse<ReadingListResponse> getMyLists(int page, int limit) {
        User me = requireAuth();
        return getUserLists(me.getId(), page, limit);
    }

    @Override
    public DatatableResponse<ReadingListResponse> getUserLists(Long userId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<ReadingListResponse> lists = listMapper.findByUser(userId, currentUserId, offset, limit);
        int total = listMapper.countByUser(userId);
        PageDataResponse<ReadingListResponse> pageData = new PageDataResponse<>(page, limit, total, lists);
        return new DatatableResponse<>(SUCCESS, "Lists retrieved", HttpStatus.OK.value(), pageData);
    }

    @Override
    public DatatableResponse<ReadingListResponse> getPublicLists(String search, String tag, int page, int limit) {
        int offset = (page - 1) * limit;
        List<ReadingListResponse> lists = listMapper.findPublicLists(search, tag, offset, limit);
        int total = listMapper.countPublicLists(search, tag);
        PageDataResponse<ReadingListResponse> pageData = new PageDataResponse<>(page, limit, total, lists);
        return new DatatableResponse<>(SUCCESS, "Public lists retrieved", HttpStatus.OK.value(), pageData);
    }

    // ── ITEMS ─────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<ReadingListItemResponse> addItem(Long listId, AddToReadingListRequest request) {
        User me = requireAuth();
        ReadingList list = listMapper.findById(listId);
        if (list == null) throw new DataNotFoundException();
        if (!list.getUserId().equals(me.getId())) throw new ForbiddenException();

        // check duplicate
        ReadingListItem existing = listMapper.findItem(listId, request.getEntityType(), request.getEntityId());
        if (existing != null) throw new BadRequestException("Item already in this list");

        // 🔧 FIX 1: Fetch entity details sebelum insert
        EntityDetails entityDetails = fetchEntityDetails(request.getEntityType(), request.getEntityId());

        ReadingListItem item = new ReadingListItem();
        item.setListId(listId);
        item.setEntityType(request.getEntityType());
        item.setEntityId(request.getEntityId());
        item.setEntitySlug(entityDetails.getSlug());
        item.setEntityTitle(entityDetails.getTitle());
        item.setEntityCover(entityDetails.getCover());
        item.setNote(request.getNote());
        item.setAddedBy(me.getId());
        item.setSortOrder(getNextSortOrder(listId));
        listMapper.insertItem(item);

        // Publish activity
        feedService.publishActivity(me.getId(), "added_to_list", request.getEntityType(),
                request.getEntityId(), item.getEntitySlug(), item.getEntityTitle(),
                item.getEntityCover(),
                "{\"listId\":" + listId + ",\"listTitle\":\"" + list.getTitle() + "\"}",
                list.getVisibility());

        // 🔧 FIX 2: Response dengan data lengkap
        ReadingListItemResponse response = mapToResponse(item);
        return new DataResponse<>(SUCCESS, "Item added", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> removeItem(Long listId, String entityType, Long entityId) {
        User me = requireAuth();
        ReadingList list = listMapper.findById(listId);
        if (list == null) throw new DataNotFoundException();
        if (!list.getUserId().equals(me.getId())) throw new ForbiddenException();

        ReadingListItem item = listMapper.findItem(listId, entityType, entityId);
        if (item == null) throw new DataNotFoundException();
        listMapper.deleteItem(listId, entityType, entityId);
        return new DataResponse<>(SUCCESS, "Item removed", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> reorderItems(Long listId, List<Long> orderedItemIds) {
        User me = requireAuth();
        ReadingList list = listMapper.findById(listId);
        if (list == null) throw new DataNotFoundException();
        if (!list.getUserId().equals(me.getId())) throw new ForbiddenException();

        AtomicInteger order = new AtomicInteger(0);
        orderedItemIds.forEach(itemId ->
                listMapper.updateItemOrder(itemId, order.getAndIncrement()));
        return new DataResponse<>(SUCCESS, "Items reordered", HttpStatus.OK.value(), null);
    }

    @Override
    public DatatableResponse<ReadingListItemResponse> getItems(Long listId, int page, int limit) {
        int offset = (page - 1) * limit;
        List<ReadingListItemResponse> items = listMapper.findItemsByList(listId, offset, limit);

        // 🔧 FIX 3: Enrich items dengan entity details jika data tidak lengkap
        enrichItemsWithEntityDetails(items);

        int total = listMapper.countItemsByList(listId);
        PageDataResponse<ReadingListItemResponse> pageData =
                new PageDataResponse<>(page, limit, total, items);
        return new DatatableResponse<>(SUCCESS, "Items retrieved", HttpStatus.OK.value(), pageData);
    }

    // ── LIKES ─────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<Void> likeList(Long listId) {
        User me = requireAuth();
        if (listMapper.isLiked(listId, me.getId())) throw new BadRequestException("Already liked");
        listMapper.insertLike(listId, me.getId());
        listMapper.incrementLikeCount(listId);

        ReadingList list = listMapper.findById(listId);
        if (list != null && !list.getUserId().equals(me.getId())) {
            notificationService.sendNotification(
                    list.getUserId(), me.getId(), "list_like",
                    "READING_LIST", listId,
                    me.getUsername() + " menyukai daftar bacaanmu \"" + list.getTitle() + "\"",
                    "{}");
        }
        return new DataResponse<>(SUCCESS, "List liked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unlikeList(Long listId) {
        User me = requireAuth();
        if (!listMapper.isLiked(listId, me.getId())) throw new DataNotFoundException();
        listMapper.deleteLike(listId, me.getId());
        listMapper.decrementLikeCount(listId);
        return new DataResponse<>(SUCCESS, "List unliked", HttpStatus.OK.value(), null);
    }

    // ── FOLLOWS ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<Void> followList(Long listId) {
        User me = requireAuth();
        if (listMapper.isFollowed(listId, me.getId())) throw new BadRequestException("Already following");
        listMapper.insertFollow(listId, me.getId());
        return new DataResponse<>(SUCCESS, "List followed", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unfollowList(Long listId) {
        User me = requireAuth();
        if (!listMapper.isFollowed(listId, me.getId())) throw new DataNotFoundException();
        listMapper.deleteFollow(listId, me.getId());
        return new DataResponse<>(SUCCESS, "List unfollowed", HttpStatus.OK.value(), null);
    }

    // ── FORK ─────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DataResponse<ReadingListResponse> forkList(Long listId) {
        User me = requireAuth();
        ReadingList original = listMapper.findById(listId);
        if (original == null) throw new DataNotFoundException();
        if (original.getUserId().equals(me.getId()))
            throw new BadRequestException("Cannot fork your own list");

        String slug = generateSlug("fork-" + original.getTitle());
        String finalSlug = slug;
        int counter = 2;
        while (listMapper.findBySlug(me.getId(), finalSlug) != null) {
            finalSlug = slug + "-" + counter++;
        }

        ReadingList forked = new ReadingList();
        forked.setUserId(me.getId());
        forked.setTitle(original.getTitle() + " (fork)");
        forked.setSlug(finalSlug);
        forked.setDescription(original.getDescription());
        forked.setVisibility("public");
        forked.setForkedFromId(original.getId());
        forked.setTags(original.getTags());
        listMapper.insertList(forked);

        // copy items - data entity details sudah tersedia dari original items
        List<ReadingListItemResponse> items = listMapper.findItemsByList(listId, 0, 1000);
        items.forEach(item -> {
            ReadingListItem newItem = new ReadingListItem();
            newItem.setListId(forked.getId());
            newItem.setEntityType(item.getEntityType());
            newItem.setEntityId(item.getEntityId());
            newItem.setEntitySlug(item.getEntitySlug());
            newItem.setEntityTitle(item.getEntityTitle());
            newItem.setEntityCover(item.getEntityCover());
            newItem.setNote(item.getNote());
            newItem.setSortOrder(item.getSortOrder());
            newItem.setAddedBy(me.getId());
            listMapper.insertItem(newItem);
        });

        listMapper.incrementForkCount(listId);

        ReadingListResponse response = listMapper.getListDetail(forked.getId(), me.getId());
        return new DataResponse<>(SUCCESS, "List forked successfully",
                HttpStatus.CREATED.value(), response);
    }

    @Override
    public DataResponse<List<ReadingListSummaryResponse>> getListsContainingEntity(
            String entityType, Long entityId) {
        Long currentUserId = currentUserIdOrNull();
        List<ReadingListSummaryResponse> lists =
                listMapper.findListsContainingEntity(entityType, entityId, currentUserId);
        return new DataResponse<>(SUCCESS, "Lists retrieved", HttpStatus.OK.value(), lists);
    }

    @Override
    public DataResponse<ReadingListResponse> getListBySlugOnly(String slug) {
        Long currentUserId = currentUserIdOrNull();
        ReadingList list = listMapper.findBySlugGlobal(slug);
        if (list == null) throw new DataNotFoundException();

        // Check visibility
        if (!list.getVisibility().equals("public")) {
            if (currentUserId == null) throw new DataNotFoundException();
            if (!list.getUserId().equals(currentUserId)) {
                // Cek apakah follower
                boolean isFollower = false;
                // Cek follow status jika visibility = followers
                if (list.getVisibility().equals("followers")) {
                    // Check if current user follows list owner
                    // isFollower = followMapper.isFollowing(currentUserId, list.getUserId());
                }
                if (!list.getUserId().equals(currentUserId) && !isFollower) {
                    throw new DataNotFoundException();
                }
            }
        }

        ReadingListResponse response = listMapper.getListDetail(list.getId(), currentUserId);

        // attach items preview (first 10)
        List<ReadingListItemResponse> items = listMapper.findItemsByList(list.getId(), 0, 10);
        enrichItemsWithEntityDetails(items);
        response.setItems(items);

        listMapper.incrementViewCount(list.getId());
        return new DataResponse<>(SUCCESS, "List retrieved", HttpStatus.OK.value(), response);
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    /**
     * Fetch entity details (slug, title, cover) based on entity type and ID
     */
    private EntityDetails fetchEntityDetails(String entityType, Long entityId) {
        EntityDetails details = new EntityDetails();

        try {
            switch (entityType.toUpperCase()) {
                case "BOOK":
                    var book = bookMapper.findById(entityId);
                    if (book != null) {
                        details.setSlug(book.getSlug());
                        details.setTitle(book.getTitle());
                        details.setCover(book.getCoverImageUrl());
                    }
                    break;
                case "FILM":
                    var film = filmMapper.findById(entityId);
                    if (film != null) {
                        details.setSlug(film.getSlug());
                        details.setTitle(film.getTitleEng());
                        details.setCover(film.getPosterUrl());
                    }
                    break;
                case "ZINE":
                    var zine = zineMapper.findById(entityId);
                    if (zine != null) {
                        details.setSlug(zine.getSlug());
                        details.setTitle(zine.getTitle());
                        details.setCover(zine.getCoverImageUrl());
                    }
                    break;
                case "NEWSPAPER":
                    var newspaper = newspaperMapper.findById(entityId);
                    if (newspaper != null) {
                        details.setSlug(newspaper.getSlug());
                        details.setTitle(newspaper.getTitle());
                    }
                    break;
                default:
                    log.warn("Unknown entity type: {}", entityType);
            }
        } catch (Exception e) {
            log.error("Error fetching entity details for type: {}, id: {}", entityType, entityId, e);
        }

        return details;
    }

    /**
     * Enrich list of items with entity details if missing
     */
    private void enrichItemsWithEntityDetails(List<ReadingListItemResponse> items) {
        if (items == null || items.isEmpty()) return;

        for (ReadingListItemResponse item : items) {
            if (item.getEntitySlug() == null && item.getEntityId() != null) {
                EntityDetails details = fetchEntityDetails(item.getEntityType(), item.getEntityId());
                if (details.getSlug() != null) {
                    item.setEntitySlug(details.getSlug());
                    item.setEntityTitle(details.getTitle());
                    item.setEntityCover(details.getCover());
                }
            }
        }
    }

    /**
     * Get next sort order for new item in a list
     */
    private int getNextSortOrder(Long listId) {
        Integer maxOrder = listMapper.getMaxSortOrder(listId);
        return (maxOrder != null ? maxOrder : -1) + 1;
    }

    /**
     * Map ReadingListItem entity to Response DTO
     */
    private ReadingListItemResponse mapToResponse(ReadingListItem item) {
        ReadingListItemResponse response = new ReadingListItemResponse();
        response.setId(item.getId());
        response.setEntityType(item.getEntityType());
        response.setEntityId(item.getEntityId());
        response.setEntitySlug(item.getEntitySlug());
        response.setEntityTitle(item.getEntityTitle());
        response.setEntityCover(item.getEntityCover());
        response.setNote(item.getNote());
        response.setSortOrder(item.getSortOrder());
        response.setAddedAt(OffsetDateTime.from(item.getCreatedAt()));
        return response;
    }

    /**
     * Inner class for entity details
     */
    private static class EntityDetails {
        private String slug;
        private String title;
        private String cover;

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCover() {
            return cover;
        }

        public void setCover(String cover) {
            this.cover = cover;
        }
    }
}