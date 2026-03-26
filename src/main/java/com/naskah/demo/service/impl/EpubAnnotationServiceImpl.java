package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.BookMapper;
import com.naskah.demo.mapper.EpubAnnotationMapper;
import com.naskah.demo.mapper.EpubBookmarkMapper;
import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.dto.request.EpubAnnotationRequest;
import com.naskah.demo.model.dto.request.EpubBookmarkRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.EpubAnnotation;
import com.naskah.demo.model.entity.EpubBookmark;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.service.EpubAnnotationService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubAnnotationServiceImpl implements EpubAnnotationService {

    private final EpubAnnotationMapper annotationMapper;
    private final EpubBookmarkMapper   bookmarkMapper;
    private final BookMapper           bookMapper;
    private final UserMapper           userMapper;
    private final HeaderHolder         headerHolder;

    private static final String SUCCESS = "Success";

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) {
            throw new UnauthorizedException();
        }
        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new UnauthorizedException();
        }
        return user;
    }

    private Book getBook(String slug) {
        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) {
            throw new DataNotFoundException();
        }
        return book;
    }

    private EpubAnnotationResponse toAnnotationResponse(EpubAnnotation a) {
        EpubAnnotationResponse r = new EpubAnnotationResponse();
        r.setId(a.getId());
        r.setCfi(a.getCfi());
        r.setSelectedText(a.getSelectedText());
        r.setColor(a.getColor());
        r.setNote(a.getNote());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }

    private EpubBookmarkResponse toBookmarkResponse(EpubBookmark b) {
        EpubBookmarkResponse r = new EpubBookmarkResponse();
        r.setId(b.getId());
        r.setCfi(b.getCfi());
        r.setLabel(b.getLabel());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    // ── Service Methods ───────────────────────────────────────────────────────

    @Override
    public DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);

        List<EpubAnnotationResponse> annotations = annotationMapper
                .findByUserAndBook(user.getId(), book.getId())
                .stream()
                .map(this::toAnnotationResponse)
                .toList();

        List<EpubBookmarkResponse> bookmarks = bookmarkMapper
                .findByUserAndBook(user.getId(), book.getId())
                .stream()
                .map(this::toBookmarkResponse)
                .toList();

        EpubAnnotationsBundleResponse bundle = new EpubAnnotationsBundleResponse();
        bundle.setAnnotations(annotations);
        bundle.setBookmarks(bookmarks);

        log.debug("EPUB bundle loaded: {} annotations, {} bookmarks for user {} book {}",
                annotations.size(), bookmarks.size(), user.getId(), bookSlug);

        return new DataResponse<>(SUCCESS, "EPUB annotations retrieved", HttpStatus.OK.value(), bundle);
    }

    @Override
    @Transactional
    public DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);

        // Default warna kuning jika frontend tidak mengirim warna
        String color = (request.getColor() != null && !request.getColor().isBlank())
                ? request.getColor()
                : "#FDE68A";

        EpubAnnotation annotation = new EpubAnnotation();
        annotation.setUserId(user.getId());
        annotation.setBookId(book.getId());
        annotation.setCfi(request.getCfi());
        annotation.setSelectedText(request.getSelectedText());
        annotation.setColor(color);
        annotation.setNote(request.getNote());
        annotation.setCreatedAt(LocalDateTime.now());
        annotation.setUpdatedAt(LocalDateTime.now());

        // Setelah insert, id ter-set otomatis via useGeneratedKeys
        annotationMapper.insert(annotation);

        log.info("EPUB annotation added: id={} user={} book={}", annotation.getId(), user.getId(), bookSlug);

        return new DataResponse<>(SUCCESS, "Annotation added", HttpStatus.CREATED.value(),
                toAnnotationResponse(annotation));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteAnnotation(String bookSlug, Long annotationId) {
        User user = getCurrentUser();

        EpubAnnotation annotation = annotationMapper.findById(annotationId);
        if (annotation == null) {
            throw new DataNotFoundException();
        }
        // Pastikan annotation milik user yang sedang login
        if (!annotation.getUserId().equals(user.getId())) {
            throw new UnauthorizedException();
        }

        annotationMapper.deleteById(annotationId);

        log.info("EPUB annotation deleted: id={} user={}", annotationId, user.getId());

        return new DataResponse<>(SUCCESS, "Annotation deleted", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<EpubBookmarkResponse> addBookmark(String bookSlug, EpubBookmarkRequest request) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);

        EpubBookmark bookmark = new EpubBookmark();
        bookmark.setUserId(user.getId());
        bookmark.setBookId(book.getId());
        bookmark.setCfi(request.getCfi());
        bookmark.setLabel(request.getLabel());
        bookmark.setCreatedAt(LocalDateTime.now());

        bookmarkMapper.insert(bookmark);

        log.info("EPUB bookmark added: id={} user={} book={}", bookmark.getId(), user.getId(), bookSlug);

        return new DataResponse<>(SUCCESS, "Bookmark added", HttpStatus.CREATED.value(),
                toBookmarkResponse(bookmark));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookmark(String bookSlug, Long bookmarkId) {
        User user = getCurrentUser();

        EpubBookmark bookmark = bookmarkMapper.findById(bookmarkId);
        if (bookmark == null) {
            throw new DataNotFoundException();
        }
        if (!bookmark.getUserId().equals(user.getId())) {
            throw new UnauthorizedException();
        }

        bookmarkMapper.deleteById(bookmarkId);

        log.info("EPUB bookmark deleted: id={} user={}", bookmarkId, user.getId());

        return new DataResponse<>(SUCCESS, "Bookmark deleted", HttpStatus.OK.value(), null);
    }
}