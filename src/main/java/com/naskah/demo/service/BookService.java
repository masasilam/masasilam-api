package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BookService {

    // Basic book operations
    DataResponse<BookResponse> createBook(BookRequest request);
    DataResponse<BookResponse> getBookDetailBySlug(String slug);
    DataResponse<ReadingResponse> startReading(String slug);
    ResponseEntity<byte[]> downloadBookAsBytes(String slug);
    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder,
                                                      String searchTitle, Long seriesId, Long genreId, Long subGenreId);
    DataResponse<Book> update(Long id, Book book, MultipartFile file) throws IOException;
    DefaultResponse delete(Long id) throws IOException;

    // Reading Progress
    DataResponse<ReadingProgressResponse> saveReadingProgress(String slug, ProgressRequest request);
    DataResponse<ReadingProgressResponse> getReadingProgress(String slug);

    // Bookmarks CRUD
    DataResponse<BookmarkResponse> addBookmark(String slug, BookmarkRequest request);
    DataResponse<List<BookmarkResponse>> getBookmarks(String slug);
    DataResponse<BookmarkResponse> updateBookmark(String slug, Long bookmarkId, BookmarkRequest request);
    DefaultResponse deleteBookmark(String slug, Long bookmarkId);

    // Highlights CRUD
    DataResponse<HighlightResponse> addHighlight(String slug, HighlightRequest request);
    DataResponse<List<HighlightResponse>> getHighlights(String slug);
    DataResponse<HighlightResponse> updateHighlight(String slug, Long highlightId, HighlightRequest request);
    DefaultResponse deleteHighlight(String slug, Long highlightId);

    // Notes CRUD
    DataResponse<NoteResponse> addNote(String slug, NoteRequest request);
    DataResponse<List<NoteResponse>> getNotes(String slug);
    DataResponse<NoteResponse> updateNote(String slug, Long noteId, NoteRequest request);
    DefaultResponse deleteNote(String slug, Long noteId);

    // Search
    DataResponse<SearchResultResponse> searchInBook(String slug, String query, int page, int limit);

    // Translation
    DataResponse<TranslationResponse> translateText(String slug, TranslationRequest request);
    DataResponse<TranslatedHighlightResponse> translateHighlight(String slug, TranslateHighlightRequest request);

    // Reactions CRUD
    DataResponse<List<ReactionResponse>> getReactions(String slug, int page, int limit);
    DataResponse<ReactionResponse> addReaction(String slug, ReactionRequest request);
    DataResponse<List<ReactionResponse>> getReactionReplies(String slug, Long reactionId);
    DataResponse<ReactionStatsResponse> getReactionStats(String slug);
    DataResponse<ReactionResponse> updateReaction(String slug, Long reactionId, ReactionRequest request);
    DataResponse<Void> removeReaction(String slug, Long reactionId);

    // TTS and Audio Sync
    DataResponse<TTSResponse> generateTextToSpeech(String slug, TTSRequest request);
    DataResponse<AudioSyncResponse> syncAudioWithText(String slug, AudioSyncRequest request);
}