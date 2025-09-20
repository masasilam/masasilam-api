package com.naskah.demo.controller;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.service.BookService;
import com.naskah.demo.util.FileTypeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // Existing endpoints
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
    public ResponseEntity<DatatableResponse<BookResponse>> getBooksPaginated(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "updateAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String searchTitle,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Long subGenreId) {

        DatatableResponse<BookResponse> response = bookService.getPaginatedBooks(
                page, limit, sortField, sortOrder, searchTitle, seriesId, genreId, subGenreId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/download")
    public ResponseEntity<byte[]> downloadBook(@PathVariable String slug) {
        return bookService.downloadBookAsBytes(slug);
    }

//    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<DataResponse<Book>> update(
//            @RequestParam String id,
//            @RequestPart("ebook") @Valid Book book,
//            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
//        DataResponse<Book> response = bookService.update(id, book, file);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping
//    public ResponseEntity<DefaultResponse> delete(@RequestParam String id) throws IOException {
//        DefaultResponse response = bookService.delete(id);
//        return ResponseEntity.ok(response);
//    }

//     ============ NEW FEATURES ============

    // 1. Simpan halaman terakhir (sync)
    @PostMapping("/{slug}/progress")
    public ResponseEntity<DataResponse<ReadingProgressResponse>> saveProgress(
            @PathVariable String slug,
            @Valid @RequestBody ProgressRequest request) {
        DataResponse<ReadingProgressResponse> response = bookService.saveReadingProgress(slug, request);
        return ResponseEntity.ok(response);
    }

    // 2. Ambil progress terakhir
    @GetMapping("/{slug}/progress")
    public ResponseEntity<DataResponse<ReadingProgressResponse>> getProgress(@PathVariable String slug) {
        DataResponse<ReadingProgressResponse> response = bookService.getReadingProgress(slug);
        return ResponseEntity.ok(response);
    }

    // 3. Bookmark halaman (sync)
    @PostMapping("/{slug}/bookmarks")
    public ResponseEntity<DataResponse<BookmarkResponse>> addBookmark(
            @PathVariable String slug,
            @Valid @RequestBody BookmarkRequest request) {
        DataResponse<BookmarkResponse> response = bookService.addBookmark(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. Ambil bookmark buku
    @GetMapping("/{slug}/bookmarks")
    public ResponseEntity<DataResponse<List<BookmarkResponse>>> getBookmarks(@PathVariable String slug) {
        DataResponse<List<BookmarkResponse>> response = bookService.getBookmarks(slug);
        return ResponseEntity.ok(response);
    }

    // 5. Pencarian kata/kalimat di buku
    @GetMapping("/{slug}/search")
    public ResponseEntity<DataResponse<SearchResultResponse>> searchInBook(
            @PathVariable String slug,
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<SearchResultResponse> response = bookService.searchInBook(slug, query, page, limit);
        return ResponseEntity.ok(response);
    }

    // 6. Highlight teks (sync)
    @PostMapping("/{slug}/highlights")
    public ResponseEntity<DataResponse<HighlightResponse>> addHighlight(
            @PathVariable String slug,
            @Valid @RequestBody HighlightRequest request) {
        DataResponse<HighlightResponse> response = bookService.addHighlight(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 7. Ambil highlights buku
    @GetMapping("/{slug}/highlights")
    public ResponseEntity<DataResponse<List<HighlightResponse>>> getHighlights(@PathVariable String slug) {
        DataResponse<List<HighlightResponse>> response = bookService.getHighlights(slug);
        return ResponseEntity.ok(response);
    }

    // 8. Catatan pribadi (sync)
    @PostMapping("/{slug}/notes")
    public ResponseEntity<DataResponse<NoteResponse>> addNote(
            @PathVariable String slug,
            @Valid @RequestBody NoteRequest request) {
        DataResponse<NoteResponse> response = bookService.addNote(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 9. Ambil catatan buku
    @GetMapping("/{slug}/notes")
    public ResponseEntity<DataResponse<List<NoteResponse>>> getNotes(@PathVariable String slug) {
        DataResponse<List<NoteResponse>> response = bookService.getNotes(slug);
        return ResponseEntity.ok(response);
    }

    // 11. Terjemahan otomatis (GRATIS)
    @PostMapping("/{slug}/translate")
    public ResponseEntity<DataResponse<TranslationResponse>> translateText(
            @PathVariable String slug,
            @Valid @RequestBody TranslationRequest request) {
        DataResponse<TranslationResponse> response = bookService.translateText(slug, request);
        return ResponseEntity.ok(response);
    }

    // 13. Highlight Auto-Translate
    @PostMapping("/{slug}/translate-highlight")
    public ResponseEntity<DataResponse<TranslatedHighlightResponse>> translateHighlight(
            @PathVariable String slug,
            @Valid @RequestBody TranslateHighlightRequest request) {
        DataResponse<TranslatedHighlightResponse> response = bookService.translateHighlight(slug, request);
        return ResponseEntity.ok(response);
    }

    // 14. Reaksi / Rating / Emoji di Buku
    @GetMapping("/{slug}/reactions")
    public ResponseEntity<DataResponse<List<ReactionResponse>>> getReactions(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        DataResponse<List<ReactionResponse>> response = bookService.getReactions(slug, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/reactions")
    public ResponseEntity<DataResponse<ReactionResponse>> addReaction(
            @PathVariable String slug,
            @RequestBody @Valid ReactionRequest request) {

        DataResponse<ReactionResponse> response = bookService.addReaction(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}/reactions/stats")
    public ResponseEntity<DataResponse<ReactionStatsResponse>> getReactionStats(@PathVariable String slug) {
        DataResponse<ReactionStatsResponse> response = bookService.getReactionStats(slug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}/reactions/{reactionId}")
    public ResponseEntity<DataResponse<Void>> removeReaction(
            @PathVariable String slug,
            @PathVariable Long reactionId) {

        DataResponse<Void> response = bookService.removeReaction(slug, reactionId);
        return ResponseEntity.ok(response);
    }

    // 15. Discussion Forum / Comments
    @GetMapping("/{slug}/discussions")
    public ResponseEntity<DataResponse<List<DiscussionResponse>>> getDiscussions(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        DataResponse<List<DiscussionResponse>> response = bookService.getDiscussions(slug, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/discussions")
    public ResponseEntity<DataResponse<DiscussionResponse>> addDiscussion(
            @PathVariable String slug,
            @Valid @RequestBody DiscussionRequest request) {
        DataResponse<DiscussionResponse> response = bookService.addDiscussion(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 16. Text-to-Speech server-side
    @PostMapping("/{slug}/tts")
    public ResponseEntity<DataResponse<TTSResponse>> generateTextToSpeech(
            @PathVariable String slug,
            @Valid @RequestBody TTSRequest request) {
        DataResponse<TTSResponse> response = bookService.generateTextToSpeech(slug, request);
        return ResponseEntity.ok(response);
    }

    // 17. Audiobook Hybrid Mode
    @PostMapping("/{slug}/sync-audio")
    public ResponseEntity<DataResponse<AudioSyncResponse>> syncAudioWithText(
            @PathVariable String slug,
            @Valid @RequestBody AudioSyncRequest request) {
        DataResponse<AudioSyncResponse> response = bookService.syncAudioWithText(slug, request);
        return ResponseEntity.ok(response);
    }
//
//    // 18. Voice Notes
//    @PostMapping("/{slug}/voice-notes")
//    public ResponseEntity<DataResponse<VoiceNoteResponse>> addVoiceNote(
//            @PathVariable String slug,
//            @RequestParam("audio") MultipartFile audioFile,
//            @RequestParam int page,
//            @RequestParam(required = false) String position) {
//        DataResponse<VoiceNoteResponse> response = bookService.addVoiceNote(slug, audioFile, page, position);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @GetMapping("/{slug}/voice-notes")
//    public ResponseEntity<DataResponse<List<VoiceNoteResponse>>> getVoiceNotes(@PathVariable String slug) {
//        DataResponse<List<VoiceNoteResponse>> response = bookService.getVoiceNotes(slug);
//        return ResponseEntity.ok(response);
//    }
//
//    // 19. Smart Vocabulary Builder
//    @PostMapping("/{slug}/vocab-builder")
//    public ResponseEntity<DataResponse<VocabularyResponse>> extractVocabulary(
//            @PathVariable String slug,
//            @Valid @RequestBody VocabularyRequest request) {
//        DataResponse<VocabularyResponse> response = bookService.extractVocabulary(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 20. AI Smart Summary per Chapter
//    @PostMapping("/{slug}/summary")
//    public ResponseEntity<DataResponse<SummaryResponse>> generateChapterSummary(
//            @PathVariable String slug,
//            @Valid @RequestBody SummaryRequest request) {
//        DataResponse<SummaryResponse> response = bookService.generateChapterSummary(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 21. AI Q&A on Book Content
//    @PostMapping("/{slug}/qa")
//    public ResponseEntity<DataResponse<QAResponse>> askQuestionAboutBook(
//            @PathVariable String slug,
//            @Valid @RequestBody QARequest request) {
//        DataResponse<QAResponse> response = bookService.askQuestionAboutBook(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 22. Comment & Reply Notes
//    @PostMapping("/{slug}/notes/{noteId}/comments")
//    public ResponseEntity<DataResponse<CommentResponse>> addCommentToNote(
//            @PathVariable String slug,
//            @PathVariable Long noteId,
//            @Valid @RequestBody CommentRequest request) {
//        DataResponse<CommentResponse> response = bookService.addCommentToNote(slug, noteId, request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    // 23. Quote Share Generator
//    @PostMapping("/{slug}/share-quote")
//    public ResponseEntity<DataResponse<ShareQuoteResponse>> generateShareableQuote(
//            @PathVariable String slug,
//            @Valid @RequestBody ShareQuoteRequest request) {
//        DataResponse<ShareQuoteResponse> response = bookService.generateShareableQuote(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 24. Highlight Trends
//    @GetMapping("/{slug}/highlights/trends")
//    public ResponseEntity<DataResponse<HighlightTrendsResponse>> getHighlightTrends(@PathVariable String slug) {
//        DataResponse<HighlightTrendsResponse> response = bookService.getHighlightTrends(slug);
//        return ResponseEntity.ok(response);
//    }
//
//    // 25. Smart Bookmark Suggestions
//    @GetMapping("/{slug}/bookmark-suggest")
//    public ResponseEntity<DataResponse<List<BookmarkSuggestionResponse>>> getBookmarkSuggestions(
//            @PathVariable String slug) {
//        DataResponse<List<BookmarkSuggestionResponse>> response = bookService.getBookmarkSuggestions(slug);
//        return ResponseEntity.ok(response);
//    }
//
//    // 26. Interactive Quizzes per Chapter
//    @PostMapping("/{slug}/quiz")
//    public ResponseEntity<DataResponse<QuizResponse>> generateChapterQuiz(
//            @PathVariable String slug,
//            @Valid @RequestBody QuizRequest request) {
//        DataResponse<QuizResponse> response = bookService.generateChapterQuiz(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 27. AI-powered Content Highlighting
//    @PostMapping("/{slug}/ai-highlight")
//    public ResponseEntity<DataResponse<AIHighlightResponse>> generateAIHighlights(
//            @PathVariable String slug,
//            @Valid @RequestBody AIHighlightRequest request) {
//        DataResponse<AIHighlightResponse> response = bookService.generateAIHighlights(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 28. Smart Notes Tagging
//    @PostMapping("/{slug}/notes/tags")
//    public ResponseEntity<DataResponse<TaggingResponse>> autoTagNotes(
//            @PathVariable String slug,
//            @Valid @RequestBody TaggingRequest request) {
//        DataResponse<TaggingResponse> response = bookService.autoTagNotes(slug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // 29. Voice-based Navigation
//    @PostMapping("/{slug}/voice-control")
//    public ResponseEntity<DataResponse<VoiceControlResponse>> processVoiceCommand(
//            @PathVariable String slug,
//            @RequestParam("audio") MultipartFile audioFile) {
//        DataResponse<VoiceControlResponse> response = bookService.processVoiceCommand(slug, audioFile);
//        return ResponseEntity.ok(response);
//    }
//
//    // 30. Realtime Collaborative Notes
//    @PostMapping("/{slug}/collab-notes")
//    public ResponseEntity<DataResponse<CollaborativeNoteResponse>> createCollaborativeNote(
//            @PathVariable String slug,
//            @Valid @RequestBody CollaborativeNoteRequest request) {
//        DataResponse<CollaborativeNoteResponse> response = bookService.createCollaborativeNote(slug, request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @GetMapping("/{slug}/collab-notes")
//    public ResponseEntity<DataResponse<List<CollaborativeNoteResponse>>> getCollaborativeNotes(
//            @PathVariable String slug) {
//        DataResponse<List<CollaborativeNoteResponse>> response = bookService.getCollaborativeNotes(slug);
//        return ResponseEntity.ok(response);
//    }
}