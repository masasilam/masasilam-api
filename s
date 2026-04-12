[1mdiff --git a/src/main/java/com/naskah/demo/controller/DashboardController.java b/src/main/java/com/naskah/demo/controller/DashboardController.java[m
[1mindex 2b79f30..919bf15 100644[m
[1m--- a/src/main/java/com/naskah/demo/controller/DashboardController.java[m
[1m+++ b/src/main/java/com/naskah/demo/controller/DashboardController.java[m
[36m@@ -183,11 +183,11 @@[m [mpublic class DashboardController {[m
     //   - active  : list GoalResponse[m
     //   - completed: list GoalResponse[m
     // ─────────────────────────────────────────────────────────────────────────[m
[31m-    @GetMapping("/goals")[m
[31m-    public ResponseEntity<DataResponse<GoalsResponse>> getGoals() {[m
[31m-        DataResponse<GoalsResponse> response = dashboardService.getGoals();[m
[31m-        return ResponseEntity.ok(response);[m
[31m-    }[m
[32m+[m[32m//    @GetMapping("/goals")[m
[32m+[m[32m//    public ResponseEntity<DataResponse<GoalsResponse>> getGoals() {[m
[32m+[m[32m//        DataResponse<GoalsResponse> response = dashboardService.getGoals();[m
[32m+[m[32m//        return ResponseEntity.ok(response);[m
[32m+[m[32m//    }[m
 [m
     @GetMapping("/recommendations")[m
     public ResponseEntity<DataResponse<List<BookRecommendationResponse>>> getRecommendations([m
[1mdiff --git a/src/main/java/com/naskah/demo/controller/book/EpubAnnotationController.java b/src/main/java/com/naskah/demo/controller/book/EpubAnnotationController.java[m
[1mindex 54dc3cb..596b5f6 100644[m
[1m--- a/src/main/java/com/naskah/demo/controller/book/EpubAnnotationController.java[m
[1m+++ b/src/main/java/com/naskah/demo/controller/book/EpubAnnotationController.java[m
[36m@@ -1,7 +1,6 @@[m
 package com.naskah.demo.controller.book;[m
 [m
[31m-import com.naskah.demo.model.dto.request.EpubAnnotationRequest;[m
[31m-import com.naskah.demo.model.dto.request.EpubBookmarkRequest;[m
[32m+[m[32mimport com.naskah.demo.model.dto.request.*;[m
 import com.naskah.demo.model.dto.response.*;[m
 import com.naskah.demo.service.EpubAnnotationService;[m
 import jakarta.validation.Valid;[m
[36m@@ -16,10 +15,11 @@[m [mimport org.springframework.web.bind.annotation.*;[m
  * Mengapa dipisah dari BookChapterController:[m
  *  - Posisi direpresentasikan sebagai CFI, bukan character offset[m
  *  - Tidak ada relasi dengan chapterNumber — EPUB bekerja lintas spine item[m
[31m- *  - Tidak memerlukan logika heatmap, analytics chapter, dsb.[m
[32m+[m[32m *  - Chapter tracking menggunakan TOC dari dalam file .epub, bukan dari DB chapter[m
  *[m
  * Base path: /api/books/{slug}/epub-annotations[m
  *             /api/books/{slug}/epub-bookmarks[m
[32m+[m[32m *             /api/books/{slug}/reading/...[m
  */[m
 @RestController[m
 @CrossOrigin(origins = "*")[m
[36m@@ -114,4 +114,74 @@[m [mpublic class EpubAnnotationController {[m
         DataResponse<Void> response = epubAnnotationService.deleteBookmark(slug, bookmarkId);[m
         return ResponseEntity.ok(response);[m
     }[m
[32m+[m
[32m+[m[32m    // ════════════════════════════════════════════════════════════[m
[32m+[m[32m    // READING SESSION[m
[32m+[m[32m    // ════════════════════════════════════════════════════════════[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * POST /api/books/{slug}/reading/epub-session[m
[32m+[m[32m     *[m
[32m+[m[32m     * Merekam sesi baca EPUB secara lengkap saat user meninggalkan halaman.[m
[32m+[m[32m     * Dikirim via dua jalur: fetch+keepalive (beforeunload) dan React cleanup.[m
[32m+[m[32m     *[m
[32m+[m[32m     * Body: EpubSessionRequest yang sudah include chapterLabel, chapterIndex, totalChapters[m
[32m+[m[32m     */[m
[32m+[m[32m    @PostMapping("/reading/epub-session")[m
[32m+[m[32m    public ResponseEntity<DataResponse<Void>> recordEpubSession([m
[32m+[m[32m            @PathVariable String slug,[m
[32m+[m[32m            @RequestBody EpubSessionRequest request) {[m
[32m+[m
[32m+[m[32m        DataResponse<Void> response = epubAnnotationService.recordEpubSession(slug, request);[m
[32m+[m[32m        return ResponseEntity.ok(response);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * POST /api/books/{slug}/reading/start[m
[32m+[m[32m     *[m
[32m+[m[32m     * Dipanggil saat EpubReaderPage pertama mount.[m
[32m+[m[32m     * Menggunakan EpubStartReadingRequest (bukan StartReadingRequest) karena:[m
[32m+[m[32m     *   - Tidak perlu chapterNumber (EPUB tidak pakai nomor chapter dari DB)[m
[32m+[m[32m     *   - Menerima chapterLabel, chapterIndex, totalChapters dari TOC .epub[m
[32m+[m[32m     *[m
[32m+[m[32m     * Auth: wajib login[m
[32m+[m[32m     */[m
[32m+[m[32m    @PostMapping("/reading/start")[m
[32m+[m[32m    public ResponseEntity<DataResponse<EpubStartReadingResponse>> startReading([m
[32m+[m[32m            @PathVariable String slug,[m
[32m+[m[32m            @Valid @RequestBody EpubStartReadingRequest request) {[m
[32m+[m
[32m+[m[32m        DataResponse<EpubStartReadingResponse> response = epubAnnotationService.startReading(slug, request);[m
[32m+[m[32m        return ResponseEntity.ok(response);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * POST /api/books/{slug}/reading/end[m
[32m+[m[32m     *[m
[32m+[m[32m     * No-op untuk EPUB — data sesi direkam via /reading/epub-session.[m
[32m+[m[32m     * Endpoint tetap tersedia untuk konsistensi API.[m
[32m+[m[32m     */[m
[32m+[m[32m    @PostMapping("/reading/end")[m
[32m+[m[32m    public ResponseEntity<DataResponse<Void>> endReading([m
[32m+[m[32m            @PathVariable String slug,[m
[32m+[m[32m            @Valid @RequestBody EndReadingRequest request) {[m
[32m+[m
[32m+[m[32m        DataResponse<Void> response = epubAnnotationService.endReading(slug, request);[m
[32m+[m[32m        return ResponseEntity.ok(response);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * POST /api/books/{slug}/reading/heartbeat[m
[32m+[m[32m     *[m
[32m+[m[32m     * Heartbeat untuk EPUB — saat ini hanya acknowledge, tidak disimpan.[m
[32m+[m[32m     * Data aktual direkam di session saat user keluar halaman.[m
[32m+[m[32m     */[m
[32m+[m[32m    @PostMapping("/reading/heartbeat")[m
[32m+[m[32m    public ResponseEntity<DataResponse<Void>> readingHeartbeat([m
[32m+[m[32m            @PathVariable String slug,[m
[32m+[m[32m            @Valid @RequestBody ReadingHeartbeatRequest request) {[m
[32m+[m
[32m+[m[32m        DataResponse<Void> response = new DataResponse<>("Success", "Heartbeat received", 200, null);[m
[32m+[m[32m        return ResponseEntity.ok(response);[m
[32m+[m[32m    }[m
 }[m
\ No newline at end of file[m
[1mdiff --git a/src/main/java/com/naskah/demo/service/impl/EpubAnnotationServiceImpl.java b/src/main/java/com/naskah/demo/service/impl/EpubAnnotationServiceImpl.java[m
[1mindex 86608e9..edf1ca8 100644[m
[1m--- a/src/main/java/com/naskah/demo/service/impl/EpubAnnotationServiceImpl.java[m
[1m+++ b/src/main/java/com/naskah/demo/service/impl/EpubAnnotationServiceImpl.java[m
[36m@@ -2,17 +2,10 @@[m [mpackage com.naskah.demo.service.impl;[m
 [m
 import com.naskah.demo.exception.custom.DataNotFoundException;[m
 import com.naskah.demo.exception.custom.UnauthorizedException;[m
[31m-import com.naskah.demo.mapper.BookMapper;[m
[31m-import com.naskah.demo.mapper.EpubAnnotationMapper;[m
[31m-import com.naskah.demo.mapper.EpubBookmarkMapper;[m
[31m-import com.naskah.demo.mapper.UserMapper;[m
[31m-import com.naskah.demo.model.dto.request.EpubAnnotationRequest;[m
[31m-import com.naskah.demo.model.dto.request.EpubBookmarkRequest;[m
[32m+[m[32mimport com.naskah.demo.mapper.*;[m
[32m+[m[32mimport com.naskah.demo.model.dto.request.*;[m
 import com.naskah.demo.model.dto.response.*;[m
[31m-import com.naskah.demo.model.entity.Book;[m
[31m-import com.naskah.demo.model.entity.EpubAnnotation;[m
[31m-import com.naskah.demo.model.entity.EpubBookmark;[m
[31m-import com.naskah.demo.model.entity.User;[m
[32m+[m[32mimport com.naskah.demo.model.entity.*;[m
 import com.naskah.demo.service.EpubAnnotationService;[m
 import com.naskah.demo.util.interceptor.HeaderHolder;[m
 import lombok.RequiredArgsConstructor;[m
[36m@@ -21,6 +14,7 @@[m [mimport org.springframework.http.HttpStatus;[m
 import org.springframework.stereotype.Service;[m
 import org.springframework.transaction.annotation.Transactional;[m
 [m
[32m+[m[32mimport java.math.BigDecimal;[m
 import java.time.LocalDateTime;[m
 import java.util.List;[m
 [m
[36m@@ -29,83 +23,53 @@[m [mimport java.util.List;[m
 @RequiredArgsConstructor[m
 public class EpubAnnotationServiceImpl implements EpubAnnotationService {[m
 [m
[31m-    private final EpubAnnotationMapper annotationMapper;[m
[31m-    private final EpubBookmarkMapper   bookmarkMapper;[m
[31m-    private final BookMapper           bookMapper;[m
[31m-    private final UserMapper           userMapper;[m
[31m-    private final HeaderHolder         headerHolder;[m
[32m+[m[32m    private final EpubAnnotationMapper  annotationMapper;[m
[32m+[m[32m    private final EpubBookmarkMapper    bookmarkMapper;[m
[32m+[m[32m    private final BookMapper            bookMapper;[m
[32m+[m[32m    private final UserMapper            userMapper;[m
[32m+[m[32m    private final ReadingSessionMapper  sessionMapper;[m
[32m+[m[32m    private final ReadingProgressMapper readingProgressMapper;[m
[32m+[m[32m    private final HeaderHolder          headerHolder;[m
 [m
[31m-    private static final String SUCCESS = "Success";[m
[31m-[m
[31m-    // ── Helpers ──────────────────────────────────────────────────────────────[m
[32m+[m[32m    private static final String SESSION_TYPE_EPUB = "EPUB";[m
[32m+[m[32m    private static final String SUCCESS           = "Success";[m
 [m
     private User getCurrentUser() {[m
         String username = headerHolder.getUsername();[m
[31m-        if (username == null || username.isBlank()) {[m
[31m-            throw new UnauthorizedException();[m
[31m-        }[m
[32m+[m[32m        if (username == null || username.isBlank()) throw new UnauthorizedException();[m
         User user = userMapper.findUserByUsername(username);[m
[31m-        if (user == null) {[m
[31m-            throw new UnauthorizedException();[m
[31m-        }[m
[32m+[m[32m        if (user == null) throw new UnauthorizedException();[m
         return user;[m
     }[m
 [m
     private Book getBook(String slug) {[m
         Book book = bookMapper.findBookBySlug(slug);[m
[31m-        if (book == null) {[m
[31m-            throw new DataNotFoundException();[m
[31m-        }[m
[32m+[m[32m        if (book == null) throw new DataNotFoundException();[m
         return book;[m
     }[m
 [m
     private EpubAnnotationResponse toAnnotationResponse(EpubAnnotation a) {[m
         EpubAnnotationResponse r = new EpubAnnotationResponse();[m
[31m-        r.setId(a.getId());[m
[31m-        r.setCfi(a.getCfi());[m
[31m-        r.setSelectedText(a.getSelectedText());[m
[31m-        r.setColor(a.getColor());[m
[31m-        r.setNote(a.getNote());[m
[31m-        r.setCreatedAt(a.getCreatedAt());[m
[31m-        r.setUpdatedAt(a.getUpdatedAt());[m
[32m+[m[32m        r.setId(a.getId()); r.setCfi(a.getCfi()); r.setSelectedText(a.getSelectedText());[m
[32m+[m[32m        r.setColor(a.getColor()); r.setNote(a.getNote());[m
[32m+[m[32m        r.setCreatedAt(a.getCreatedAt()); r.setUpdatedAt(a.getUpdatedAt());[m
         return r;[m
     }[m
 [m
     private EpubBookmarkResponse toBookmarkResponse(EpubBookmark b) {[m
         EpubBookmarkResponse r = new EpubBookmarkResponse();[m
[31m-        r.setId(b.getId());[m
[31m-        r.setCfi(b.getCfi());[m
[31m-        r.setLabel(b.getLabel());[m
[31m-        r.setCreatedAt(b.getCreatedAt());[m
[32m+[m[32m        r.setId(b.getId()); r.setCfi(b.getCfi()); r.setLabel(b.getLabel()); r.setCreatedAt(b.getCreatedAt());[m
         return r;[m
     }[m
 [m
[31m-    // ── Service Methods ───────────────────────────────────────────────────────[m
[31m-[m
     @Override[m
     public DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug) {[m
         User user = getCurrentUser();[m
         Book book = getBook(bookSlug);[m
[31m-[m
[31m-        List<EpubAnnotationResponse> annotations = annotationMapper[m
[31m-                .findByUserAndBook(user.getId(), book.getId())[m
[31m-                .stream()[m
[31m-                .map(this::toAnnotationResponse)[m
[31m-                .toList();[m
[31m-[m
[31m-        List<EpubBookmarkResponse> bookmarks = bookmarkMapper[m
[31m-                .findByUserAndBook(user.getId(), book.getId())[m
[31m-                .stream()[m
[31m-                .map(this::toBookmarkResponse)[m
[31m-                .toList();[m
[31m-[m
[32m+[m[32m        List<EpubAnnotationResponse> annotations = annotationMapper.findByUserAndBook(user.getId(), book.getId()).stream().map(this::toAnnotationResponse).toList();[m
[32m+[m[32m        List<EpubBookmarkResponse> bookmarks = bookmarkMapper.findByUserAndBook(user.getId(), book.getId()).stream().map(this::toBookmarkResponse).toList();[m
         EpubAnnotationsBundleResponse bundle = new EpubAnnotationsBundleResponse();[m
[31m-        bundle.setAnnotations(annotations);[m
[31m-        bundle.setBookmarks(bookmarks);[m
[31m-[m
[31m-        log.debug("EPUB bundle loaded: {} annotations, {} bookmarks for user {} book {}",[m
[31m-                annotations.size(), bookmarks.size(), user.getId(), bookSlug);[m
[31m-[m
[32m+[m[32m        bundle.setAnnotations(annotations); bundle.setBookmarks(bookmarks);[m
         return new DataResponse<>(SUCCESS, "EPUB annotations retrieved", HttpStatus.OK.value(), bundle);[m
     }[m
 [m
[36m@@ -114,49 +78,25 @@[m [mpublic class EpubAnnotationServiceImpl implements EpubAnnotationService {[m
     public DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request) {[m
         User user = getCurrentUser();[m
         Book book = getBook(bookSlug);[m
[31m-[m
[31m-        // Default warna kuning jika frontend tidak mengirim warna[m
[31m-        String color = (request.getColor() != null && !request.getColor().isBlank())[m
[31m-                ? request.getColor()[m
[31m-                : "#FDE68A";[m
[31m-[m
[32m+[m[32m        String color = (request.getColor() != null && !request.getColor().isBlank()) ? request.getColor() : "#FDE68A";[m
         EpubAnnotation annotation = new EpubAnnotation();[m
[31m-        annotation.setUserId(user.getId());[m
[31m-        annotation.setBookId(book.getId());[m
[31m-        annotation.setCfi(request.getCfi());[m
[31m-        annotation.setSelectedText(request.getSelectedText());[m
[31m-        annotation.setColor(color);[m
[31m-        annotation.setNote(request.getNote());[m
[31m-        annotation.setCreatedAt(LocalDateTime.now());[m
[31m-        annotation.setUpdatedAt(LocalDateTime.now());[m
[31m-[m
[31m-        // Setelah insert, id ter-set otomatis via useGeneratedKeys[m
[32m+[m[32m        annotation.setUserId(user.getId()); annotation.setBookId(book.getId());[m
[32m+[m[32m        annotation.setCfi(request.getCfi()); annotation.setSelectedText(request.getSelectedText());[m
[32m+[m[32m        annotation.setColor(color); annotation.